/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.requiredaction;

import io.github.qcodr.keycloak.bulkgate.config.ConfigKeys;
import io.github.qcodr.keycloak.bulkgate.phone.InvalidPhoneNumberException;
import io.github.qcodr.keycloak.bulkgate.phone.LibPhoneNumberNormalizer;
import io.github.qcodr.keycloak.bulkgate.phone.PhoneNumber;
import io.github.qcodr.keycloak.bulkgate.phone.PhoneNumberNormalizer;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.UserModel;

/**
 * Enrollment step that collects and stores the user's mobile number as a user
 * attribute, so the SMS OTP authenticator has a number to send to.
 *
 * <p>The number is normalised to E.164 before storage. As a required action has
 * no access to the authenticator's per-realm config, it uses the default country
 * code and attribute name; see the README for the customization caveat.</p>
 */
public class PhoneNumberRequiredAction implements RequiredActionProvider {

    public static final String PROVIDER_ID = "bulkgate-phone-number-config";

    static final String FORM = "phone-number-config.ftl";
    static final String FORM_FIELD = "mobileNumber";
    static final String MSG_INVALID_PHONE = "bulkgateInvalidPhone";

    private final PhoneNumberNormalizer normalizer =
            new LibPhoneNumberNormalizer(ConfigKeys.DEFAULT_COUNTRY_CODE_VALUE);

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        // The trigger is added explicitly by the authenticator; nothing to evaluate.
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        String current = context.getUser().getFirstAttribute(ConfigKeys.DEFAULT_PHONE_NUMBER_ATTRIBUTE);
        Response form = context.form()
                .setAttribute("mobileNumber", current == null ? "" : current)
                .createForm(FORM);
        context.challenge(form);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String raw = formData.getFirst(FORM_FIELD);

        try {
            PhoneNumber phone = normalizer.normalize(raw);
            UserModel user = context.getUser();
            user.setSingleAttribute(ConfigKeys.DEFAULT_PHONE_NUMBER_ATTRIBUTE, phone.e164());
            // A freshly entered number is not yet proven; the OTP step verifies it.
            user.setSingleAttribute(ConfigKeys.DEFAULT_PHONE_NUMBER_VERIFIED_ATTRIBUTE, "false");
            context.success();
        } catch (InvalidPhoneNumberException e) {
            Response form = context.form()
                    .setAttribute("mobileNumber", raw == null ? "" : raw)
                    .setError(MSG_INVALID_PHONE)
                    .createForm(FORM);
            context.challenge(form);
        }
    }

    @Override
    public void close() {}
}
