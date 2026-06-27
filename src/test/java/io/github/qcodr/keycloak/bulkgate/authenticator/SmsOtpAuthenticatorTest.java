/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.authenticator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.qcodr.keycloak.bulkgate.config.ConfigKeys;
import io.github.qcodr.keycloak.bulkgate.gateway.SmsGateway;
import io.github.qcodr.keycloak.bulkgate.gateway.SmsMessage;
import io.github.qcodr.keycloak.bulkgate.gateway.SmsSendResult;
import io.github.qcodr.keycloak.bulkgate.otp.OtpCode;
import io.github.qcodr.keycloak.bulkgate.otp.OtpCodeGenerator;
import io.github.qcodr.keycloak.bulkgate.otp.SaltSource;
import io.github.qcodr.keycloak.bulkgate.requiredaction.PhoneNumberRequiredAction;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Behavioural unit tests for the orchestration in {@link SmsOtpAuthenticator}.
 *
 * <p>The deterministic collaborators (fixed clock, a generator that always emits
 * {@code 123456}, a constant salt) make every OTP issuance reproducible, while a
 * {@link SmsGatewayResolver} mock returns a stubbed {@link SmsGateway} so no
 * network is touched and the outgoing {@link SmsMessage} can be captured. The
 * authentication session's note API is backed by a real {@link HashMap}, so a
 * challenge stored during {@code authenticate()} is read back in {@code action()}
 * exactly as it would be at runtime. Strictness is LENIENT because the shared
 * setup stubs more than any single branch consumes.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SmsOtpAuthenticatorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final String VALID_PHONE = "+36201234567";
    private static final String FIXED_CODE = "123456";

    @Mock
    private AuthenticationFlowContext context;

    @Mock
    private AuthenticatorConfigModel configModel;

    @Mock
    private UserModel user;

    @Mock
    private AuthenticationSessionModel authSession;

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private LoginFormsProvider form;

    @Mock
    private EventBuilder event;

    @Mock
    private SmsGatewayResolver gatewayResolver;

    @Mock
    private SmsGateway gateway;

    @Mock
    private Response formResponse;

    @Mock
    private Response errorResponse;

    // Deterministic collaborators injected through the package-private constructor.
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final OtpCodeGenerator codeGenerator = length -> new OtpCode(FIXED_CODE);
    private final SaltSource saltSource = () -> "fixed-salt";

    private Map<String, String> config;
    private Map<String, String> authNotes;
    private SmsOtpAuthenticator authenticator;

    @BeforeEach
    void setUp() throws Exception {
        config = baseConfig();
        authNotes = new HashMap<>();
        authenticator = new SmsOtpAuthenticator(clock, codeGenerator, saltSource, gatewayResolver, new ObjectMapper());

        // Flow context wiring.
        when(context.getAuthenticatorConfig()).thenReturn(configModel);
        when(configModel.getConfig()).thenReturn(config);
        when(context.getUser()).thenReturn(user);
        when(context.getAuthenticationSession()).thenReturn(authSession);
        when(context.getHttpRequest()).thenReturn(httpRequest);
        when(context.form()).thenReturn(form);
        when(context.getEvent()).thenReturn(event);
        // user(..) is overloaded (UserModel / String); pin the UserModel overload the code uses.
        when(event.user(any(UserModel.class))).thenReturn(event); // error(String) is void; only user(..) chains

        // Fluent form: every builder call returns the same provider, terminals return a Response.
        when(form.setAttribute(any(), any())).thenReturn(form);
        when(form.setError(anyString())).thenReturn(form);
        when(form.setInfo(anyString())).thenReturn(form);
        when(form.createForm(anyString())).thenReturn(formResponse);
        when(form.createErrorPage(any())).thenReturn(errorResponse);

        // Gateway is resolved via the mock resolver and accepts by default.
        when(gatewayResolver.resolve(any())).thenReturn(gateway);
        when(gateway.send(any())).thenReturn(SmsSendResult.accepted("provider-id"));

        // Back the auth-note API with a real map so store -> load round-trips.
        doAnswer(inv -> {
                    authNotes.put(inv.getArgument(0), inv.getArgument(1));
                    return null;
                })
                .when(authSession)
                .setAuthNote(anyString(), anyString());
        when(authSession.getAuthNote(anyString())).thenAnswer(inv -> authNotes.get(inv.getArgument(0)));
        doAnswer(inv -> {
                    authNotes.remove(inv.getArgument(0));
                    return null;
                })
                .when(authSession)
                .removeAuthNote(anyString());
    }

    private Map<String, String> baseConfig() {
        Map<String, String> c = new HashMap<>();
        c.put(ConfigKeys.PHONE_NUMBER_ATTRIBUTE, "phoneNumber");
        c.put(ConfigKeys.PHONE_NUMBER_VERIFIED_ATTRIBUTE, "phoneNumberVerified");
        c.put(ConfigKeys.MARK_PHONE_VERIFIED, "true");
        c.put(ConfigKeys.CODE_LENGTH, "6");
        c.put(ConfigKeys.CODE_TTL_SECONDS, "300");
        c.put(ConfigKeys.MAX_VERIFY_ATTEMPTS, "3");
        c.put(ConfigKeys.RESEND_COOLDOWN_SECONDS, "30");
        c.put(ConfigKeys.MAX_RESENDS, "3");
        c.put(ConfigKeys.DEFAULT_COUNTRY_CODE, "+36");
        // Non-blank override keeps resolveSmsTemplate off the theme/session path.
        c.put(ConfigKeys.SMS_TEXT_TEMPLATE, "Code %code% valid %ttl%");
        c.put(ConfigKeys.SIMULATION_MODE, "false");
        c.put(ConfigKeys.BULKGATE_APPLICATION_ID, "dummy-app-id");
        c.put(ConfigKeys.BULKGATE_APPLICATION_TOKEN, "dummy-app-token");
        return c;
    }

    private MultivaluedHashMap<String, String> codeParams(String code) {
        MultivaluedHashMap<String, String> params = new MultivaluedHashMap<>();
        params.add(SmsOtpAuthenticator.FORM_CODE_PARAM, code);
        return params;
    }

    private MultivaluedHashMap<String, String> resendParams() {
        MultivaluedHashMap<String, String> params = new MultivaluedHashMap<>();
        params.add(SmsOtpAuthenticator.FORM_RESEND_PARAM, "true");
        return params;
    }

    // ----------------------------------------------------------- authenticate ---

    @Test
    @DisplayName("authenticate: no stored phone -> add enrollment required action and succeed without sending")
    void authenticate_noPhone_defersToRequiredAction() throws Exception {
        when(user.getFirstAttribute("phoneNumber")).thenReturn(null);

        authenticator.authenticate(context);

        verify(user).addRequiredAction(PhoneNumberRequiredAction.PROVIDER_ID);
        verify(context).success();
        verify(gateway, never()).send(any());
        verify(context, never()).challenge(any());
        assertThat(authNotes).doesNotContainKey(OtpChallengeStore.NOTE);
    }

    @Test
    @DisplayName("authenticate: unparseable stored phone -> failureChallenge, nothing sent")
    void authenticate_invalidStoredPhone_failsChallenge() throws Exception {
        when(user.getFirstAttribute("phoneNumber")).thenReturn("not-a-number");

        authenticator.authenticate(context);

        verify(context).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), any());
        verify(form).setError(SmsOtpAuthenticator.MSG_INVALID_PHONE);
        verify(gateway, never()).send(any());
        verify(context, never()).success();
    }

    @Test
    @DisplayName("authenticate: valid phone -> sends one SMS to that number containing the code and challenges")
    void authenticate_validPhone_sendsCodeAndChallenges() throws Exception {
        when(user.getFirstAttribute("phoneNumber")).thenReturn(VALID_PHONE);

        authenticator.authenticate(context);

        ArgumentCaptor<SmsMessage> sent = ArgumentCaptor.forClass(SmsMessage.class);
        verify(gateway).send(sent.capture());
        assertThat(sent.getValue().recipient()).isEqualTo(VALID_PHONE);
        assertThat(sent.getValue().text()).contains(FIXED_CODE);
        // ttlMinutes for a 300s TTL is 5; the override template renders both fields.
        assertThat(sent.getValue().text()).isEqualTo("Code 123456 valid 5");

        verify(context).challenge(formResponse);
        verify(context, never()).success();
        assertThat(authNotes.get(OtpChallengeStore.NOTE)).isNotNull();
    }

    @Test
    @DisplayName("authenticate: gateway rejects the send -> failureChallenge and the stored challenge is cleared")
    void authenticate_gatewayRejects_failsAndClearsChallenge() throws Exception {
        when(user.getFirstAttribute("phoneNumber")).thenReturn(VALID_PHONE);
        when(gateway.send(any())).thenReturn(SmsSendResult.rejected("400", "no"));

        authenticator.authenticate(context);

        verify(context).failureChallenge(eq(AuthenticationFlowError.INTERNAL_ERROR), any());
        verify(form).setError(SmsOtpAuthenticator.MSG_NOT_SENT);
        // The hash must not survive a code the user never received.
        assertThat(authNotes.get(OtpChallengeStore.NOTE)).isNull();
        verify(context, never()).success();
    }

    // ----------------------------------------------------------------- action ---

    @Test
    @DisplayName("action: correct code -> success and phone stamped verified")
    void action_correctCode_succeedsAndMarksVerified() throws Exception {
        when(user.getFirstAttribute("phoneNumber")).thenReturn(VALID_PHONE);
        authenticator.authenticate(context); // issues + stores the challenge

        when(httpRequest.getDecodedFormParameters()).thenReturn(codeParams(FIXED_CODE));
        authenticator.action(context);

        verify(context).success();
        verify(user).setSingleAttribute("phoneNumberVerified", "true");
        // Consumed challenge is cleared so the code cannot be replayed.
        assertThat(authNotes.get(OtpChallengeStore.NOTE)).isNull();
        verify(context, never()).failureChallenge(any(), any());
    }

    @Test
    @DisplayName("action: one wrong code -> re-challenge with invalid message, no success, not verified")
    void action_wrongCode_reChallengesWithoutSuccess() throws Exception {
        when(user.getFirstAttribute("phoneNumber")).thenReturn(VALID_PHONE);
        authenticator.authenticate(context);

        when(httpRequest.getDecodedFormParameters()).thenReturn(codeParams("000000"));
        authenticator.action(context);

        verify(context, never()).success();
        verify(user, never()).setSingleAttribute(eq("phoneNumberVerified"), anyString());
        verify(form).setError(SmsOtpAuthenticator.MSG_CODE_INVALID);
        // One challenge from authenticate() + one re-challenge from the wrong submission.
        verify(context, times(2)).challenge(any());
        verify(context, never()).failureChallenge(any(), any());
        // The challenge is retained (with an incremented attempt counter) for retry.
        assertThat(authNotes.get(OtpChallengeStore.NOTE)).isNotNull();
    }

    @Test
    @DisplayName("action: exhausting maxVerifyAttempts wrong codes -> failureChallenge with too-many-attempts")
    void action_tooManyWrongCodes_failsWithLimitMessage() throws Exception {
        when(user.getFirstAttribute("phoneNumber")).thenReturn(VALID_PHONE);
        authenticator.authenticate(context);
        when(httpRequest.getDecodedFormParameters()).thenReturn(codeParams("000000"));

        // maxVerifyAttempts = 3: the third wrong submission trips the limit.
        authenticator.action(context);
        authenticator.action(context);
        authenticator.action(context);

        verify(context).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), any());
        verify(form).setError(SmsOtpAuthenticator.MSG_TOO_MANY);
        verify(context, never()).success();
        verify(user, never()).setSingleAttribute(eq("phoneNumberVerified"), anyString());
    }

    @Test
    @DisplayName("action: resend within cooldown -> re-challenge with cooldown message, no second SMS")
    void action_resendDuringCooldown_reChallengesWithoutSending() throws Exception {
        when(user.getFirstAttribute("phoneNumber")).thenReturn(VALID_PHONE);
        authenticator.authenticate(context); // send #1 at NOW, lastSentAt == NOW

        // Default 30s cooldown + fixed clock => COOLDOWN branch of ResendPolicy.
        when(httpRequest.getDecodedFormParameters()).thenReturn(resendParams());
        authenticator.action(context);

        verify(gateway, times(1)).send(any()); // still only the original send
        verify(form).setError(SmsOtpAuthenticator.MSG_RESEND_COOLDOWN);
        verify(context, never()).success();
    }

    @Test
    @DisplayName("action: resend with cooldown disabled -> issues a new code and sends a second SMS")
    void action_resendAllowed_sendsSecondCode() throws Exception {
        config.put(ConfigKeys.RESEND_COOLDOWN_SECONDS, "0"); // ALLOWED branch of ResendPolicy
        when(user.getFirstAttribute("phoneNumber")).thenReturn(VALID_PHONE);
        authenticator.authenticate(context); // send #1

        when(httpRequest.getDecodedFormParameters()).thenReturn(resendParams());
        authenticator.action(context);

        ArgumentCaptor<SmsMessage> sent = ArgumentCaptor.forClass(SmsMessage.class);
        verify(gateway, times(2)).send(sent.capture()); // original + resend
        assertThat(sent.getValue().recipient()).isEqualTo(VALID_PHONE);
        assertThat(sent.getValue().text()).contains(FIXED_CODE);
        verify(form).setInfo(SmsOtpAuthenticator.MSG_RESENT);
        verify(context, never()).success();
    }
}
