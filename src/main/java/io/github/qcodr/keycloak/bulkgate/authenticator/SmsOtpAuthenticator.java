/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.authenticator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.qcodr.keycloak.bulkgate.config.ConfigKeys;
import io.github.qcodr.keycloak.bulkgate.config.InvalidConfigurationException;
import io.github.qcodr.keycloak.bulkgate.config.SmsAuthenticatorConfig;
import io.github.qcodr.keycloak.bulkgate.gateway.SmsGateway;
import io.github.qcodr.keycloak.bulkgate.gateway.SmsGatewayException;
import io.github.qcodr.keycloak.bulkgate.gateway.SmsMessage;
import io.github.qcodr.keycloak.bulkgate.gateway.SmsSendResult;
import io.github.qcodr.keycloak.bulkgate.message.SmsTextFormatter;
import io.github.qcodr.keycloak.bulkgate.otp.IssuedOtp;
import io.github.qcodr.keycloak.bulkgate.otp.OtpChallenge;
import io.github.qcodr.keycloak.bulkgate.otp.OtpChallengeFactory;
import io.github.qcodr.keycloak.bulkgate.otp.OtpCodeGenerator;
import io.github.qcodr.keycloak.bulkgate.otp.OtpVerificationResult;
import io.github.qcodr.keycloak.bulkgate.otp.OtpVerifier;
import io.github.qcodr.keycloak.bulkgate.otp.ResendPolicy;
import io.github.qcodr.keycloak.bulkgate.otp.SaltSource;
import io.github.qcodr.keycloak.bulkgate.otp.SecureRandomOtpCodeGenerator;
import io.github.qcodr.keycloak.bulkgate.otp.SecureRandomSaltSource;
import io.github.qcodr.keycloak.bulkgate.otp.Sha256OtpHasher;
import io.github.qcodr.keycloak.bulkgate.phone.InvalidPhoneNumberException;
import io.github.qcodr.keycloak.bulkgate.phone.LibPhoneNumberNormalizer;
import io.github.qcodr.keycloak.bulkgate.phone.PhoneNumber;
import io.github.qcodr.keycloak.bulkgate.requiredaction.PhoneNumberRequiredAction;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

/**
 * SMS one-time-password authenticator. Keycloak generates the code, sends it via
 * BulkGate, and verifies it locally — so the security boundary stays in Keycloak
 * and BulkGate is purely the SMS transport.
 *
 * <p>Usable as either a second factor (after password) or a first factor (after a
 * username form), selected by where it is bound in the flow. All decision logic
 * lives in small, separately tested collaborators ({@link OtpVerifier},
 * {@link ResendPolicy}, {@link OtpChallengeFactory}); this class is the thin
 * orchestration between them and Keycloak.</p>
 */
public class SmsOtpAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(SmsOtpAuthenticator.class);

    static final String FORM = "login-sms-otp.ftl";
    static final String FORM_CODE_PARAM = "code";
    static final String FORM_RESEND_PARAM = "resend";

    // Error / info message keys (see theme-resources/messages/messages_en.properties).
    static final String MSG_NOT_SENT = "bulkgateSmsNotSent";
    static final String MSG_CODE_INVALID = "bulkgateCodeInvalid";
    static final String MSG_CODE_EXPIRED = "bulkgateCodeExpired";
    static final String MSG_TOO_MANY = "bulkgateTooManyAttempts";
    static final String MSG_INVALID_PHONE = "bulkgateInvalidPhone";
    static final String MSG_RESENT = "bulkgateCodeResent";
    static final String MSG_RESEND_COOLDOWN = "bulkgateResendCooldown";
    static final String MSG_RESEND_LIMIT = "bulkgateResendLimit";
    static final String MSG_INTERNAL = "bulkgateInternalError";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Clock clock;
    private final OtpChallengeFactory challengeFactory;
    private final OtpVerifier verifier;
    private final SmsTextFormatter textFormatter;
    private final OtpChallengeStore challengeStore;
    private final SmsGatewayResolver gatewayResolver;

    /** Production wiring: secure-random code/salt, system clock, shared HTTP client. */
    public SmsOtpAuthenticator() {
        this(Clock.systemUTC(),
                new SecureRandomOtpCodeGenerator(),
                new SecureRandomSaltSource(),
                new SmsGatewayResolver(newHttpClient(), MAPPER, Duration.ofSeconds(10)),
                MAPPER);
    }

    SmsOtpAuthenticator(Clock clock,
                        OtpCodeGenerator codeGenerator,
                        SaltSource saltSource,
                        SmsGatewayResolver gatewayResolver,
                        ObjectMapper mapper) {
        Sha256OtpHasher hasher = new Sha256OtpHasher();
        this.clock = clock;
        this.challengeFactory = new OtpChallengeFactory(codeGenerator, hasher, saltSource, clock);
        this.verifier = new OtpVerifier(hasher);
        this.textFormatter = new SmsTextFormatter();
        this.challengeStore = new OtpChallengeStore(mapper);
        this.gatewayResolver = gatewayResolver;
    }

    private static HttpClient newHttpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    // ---------------------------------------------------------------- send ---

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        SmsAuthenticatorConfig config = parseConfig(context);
        if (config == null) {
            return;
        }
        UserModel user = context.getUser();
        String rawPhone = user.getFirstAttribute(config.phoneNumberAttribute());
        if (rawPhone == null || rawPhone.isBlank()) {
            // No number on file: defer to the enrollment required action.
            user.addRequiredAction(PhoneNumberRequiredAction.PROVIDER_ID);
            context.success();
            return;
        }

        PhoneNumber phone;
        try {
            phone = new LibPhoneNumberNormalizer(config.defaultCountryCode()).normalize(rawPhone);
        } catch (InvalidPhoneNumberException e) {
            LOG.warnf("Stored phone number for user %s is not valid: %s", user.getId(), e.getMessage());
            failure(context, AuthenticationFlowError.INVALID_CREDENTIALS, MSG_INVALID_PHONE, Response.Status.BAD_REQUEST);
            return;
        }
        issueAndChallenge(context, config, phone, 0, 0, null);
    }

    private void issueAndChallenge(AuthenticationFlowContext context, SmsAuthenticatorConfig config,
                                   PhoneNumber phone, int priorResends, int priorAttempts, String infoKey) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        IssuedOtp issued = challengeFactory.issue(
                config.codeLength(), config.codeTtl(), phone.e164(), priorResends, priorAttempts);
        challengeStore.store(authSession, issued.challenge());

        String text = textFormatter.format(config.smsTextTemplate(), issued.plaintext().value(), config.ttlMinutes());
        try {
            SmsGateway gateway = gatewayResolver.resolve(config);
            SmsSendResult result = gateway.send(new SmsMessage(phone.e164(), text));
            if (!result.accepted()) {
                LOG.warnf("BulkGate rejected SMS: code=%s message=%s", result.errorCode(), result.errorMessage());
                challengeStore.clear(authSession);
                failure(context, AuthenticationFlowError.INTERNAL_ERROR, MSG_NOT_SENT, Response.Status.INTERNAL_SERVER_ERROR);
                return;
            }
        } catch (SmsGatewayException | RuntimeException e) {
            LOG.error("Failed to send OTP via BulkGate", e);
            // Don't leave a hash for a code the user never received.
            challengeStore.clear(authSession);
            failure(context, AuthenticationFlowError.INTERNAL_ERROR, MSG_NOT_SENT, Response.Status.INTERNAL_SERVER_ERROR);
            return;
        }

        LoginFormsProvider form = context.form().setAttribute("ttlMinutes", config.ttlMinutes());
        if (infoKey != null) {
            form.setInfo(infoKey);
        }
        context.challenge(form.createForm(FORM));
    }

    // -------------------------------------------------------------- verify ---

    @Override
    public void action(AuthenticationFlowContext context) {
        SmsAuthenticatorConfig config = parseConfig(context);
        if (config == null) {
            return;
        }
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        MultivaluedMap<String, String> form = context.getHttpRequest().getDecodedFormParameters();

        if (form.containsKey(FORM_RESEND_PARAM)) {
            handleResend(context, config, authSession);
            return;
        }

        String submitted = form.getFirst(FORM_CODE_PARAM);
        OtpChallenge challenge = challengeStore.load(authSession);
        OtpVerificationResult result = verifier.verify(challenge, submitted, clock.instant(), config.maxVerifyAttempts());

        switch (result) {
            case VALID -> {
                challengeStore.clear(authSession);
                markPhoneVerified(context, config);
                context.success();
            }
            case INVALID -> onInvalidCode(context, config, authSession, challenge);
            case EXPIRED ->
                    failure(context, AuthenticationFlowError.EXPIRED_CODE, MSG_CODE_EXPIRED, Response.Status.BAD_REQUEST);
            case TOO_MANY_ATTEMPTS -> {
                context.getEvent().user(context.getUser()).error(Errors.INVALID_USER_CREDENTIALS);
                failure(context, AuthenticationFlowError.INVALID_CREDENTIALS, MSG_TOO_MANY, Response.Status.BAD_REQUEST);
            }
            case NO_CHALLENGE ->
                    failure(context, AuthenticationFlowError.INTERNAL_ERROR, MSG_INTERNAL, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Records that the phone number is now proven, so the standard OIDC
     * {@code phone_number_verified} claim (and conditional flows) can rely on it.
     */
    private void markPhoneVerified(AuthenticationFlowContext context, SmsAuthenticatorConfig config) {
        if (config.markPhoneVerified()) {
            context.getUser().setSingleAttribute(config.phoneNumberVerifiedAttribute(), "true");
        }
    }

    private void onInvalidCode(AuthenticationFlowContext context, SmsAuthenticatorConfig config,
                               AuthenticationSessionModel authSession, OtpChallenge challenge) {
        OtpChallenge updated = challenge.withIncrementedAttempts();
        challengeStore.store(authSession, updated);
        context.getEvent().user(context.getUser()).error(Errors.INVALID_USER_CREDENTIALS);

        if (updated.attempts() >= config.maxVerifyAttempts()) {
            failure(context, AuthenticationFlowError.INVALID_CREDENTIALS, MSG_TOO_MANY, Response.Status.BAD_REQUEST);
        } else {
            reChallenge(context, config, MSG_CODE_INVALID);
        }
    }

    private void handleResend(AuthenticationFlowContext context, SmsAuthenticatorConfig config,
                              AuthenticationSessionModel authSession) {
        OtpChallenge challenge = challengeStore.load(authSession);
        if (challenge == null) {
            failure(context, AuthenticationFlowError.INTERNAL_ERROR, MSG_INTERNAL, Response.Status.INTERNAL_SERVER_ERROR);
            return;
        }
        ResendPolicy.Decision decision =
                ResendPolicy.evaluate(challenge, config.maxResends(), config.resendCooldown(), clock.instant());
        switch (decision) {
            case ALLOWED ->
                    // Carry the spent attempts forward so a resend cannot reset the
                    // guessing budget; the budget is per login session, not per code.
                    issueAndChallenge(context, config, new PhoneNumber(challenge.recipient()),
                            challenge.resends() + 1, challenge.attempts(), MSG_RESENT);
            case COOLDOWN -> reChallenge(context, config, MSG_RESEND_COOLDOWN);
            case LIMIT_REACHED -> reChallenge(context, config, MSG_RESEND_LIMIT);
        }
    }

    // -------------------------------------------------------------- helpers --

    private void reChallenge(AuthenticationFlowContext context, SmsAuthenticatorConfig config, String errorKey) {
        Response form = context.form()
                .setAttribute("ttlMinutes", config.ttlMinutes())
                .setError(errorKey)
                .createForm(FORM);
        context.challenge(form);
    }

    private void failure(AuthenticationFlowContext context, AuthenticationFlowError error,
                         String messageKey, Response.Status status) {
        context.failureChallenge(error, context.form().setError(messageKey).createErrorPage(status));
    }

    private SmsAuthenticatorConfig parseConfig(AuthenticationFlowContext context) {
        try {
            AuthenticatorConfigModel model = context.getAuthenticatorConfig();
            return SmsAuthenticatorConfig.from(model == null ? null : model.getConfig());
        } catch (InvalidConfigurationException e) {
            LOG.error("Invalid BulkGate SMS authenticator configuration", e);
            failure(context, AuthenticationFlowError.INTERNAL_ERROR, MSG_INTERNAL, Response.Status.INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    // ------------------------------------------------------- SPI lifecycle ---

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // No per-execution config is available here, so the default attribute name
        // is used. See README for the implication of customizing the attribute name.
        String phone = user.getFirstAttribute(ConfigKeys.DEFAULT_PHONE_NUMBER_ATTRIBUTE);
        return phone != null && !phone.isBlank();
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction(PhoneNumberRequiredAction.PROVIDER_ID);
    }

    @Override
    public void close() {
    }
}
