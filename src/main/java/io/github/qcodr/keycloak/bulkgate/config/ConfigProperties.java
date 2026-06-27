/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.config;

import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * The per-realm configuration fields shown in the Keycloak admin console for
 * this authenticator. Defined alongside {@link ConfigKeys} so the field list and
 * the parsing logic share one set of key/default constants.
 */
public final class ConfigProperties {

    private ConfigProperties() {
    }

    public static List<ProviderConfigProperty> list() {
        ProviderConfigProperty token = string(
                ConfigKeys.BULKGATE_APPLICATION_TOKEN,
                "BulkGate application token",
                "The Application Token from your BulkGate API application. Required unless simulation mode is on.",
                "");
        token.setSecret(true);

        return List.of(
                // --- OTP behaviour ---
                string(ConfigKeys.CODE_LENGTH, "Code length",
                        "Number of digits in the generated code (" + ConfigKeys.MIN_CODE_LENGTH
                                + "-" + ConfigKeys.MAX_CODE_LENGTH + ").",
                        Integer.toString(ConfigKeys.DEFAULT_CODE_LENGTH)),
                string(ConfigKeys.CODE_TTL_SECONDS, "Code time-to-live (seconds)",
                        "How long a sent code stays valid.",
                        Integer.toString(ConfigKeys.DEFAULT_CODE_TTL_SECONDS)),
                string(ConfigKeys.MAX_VERIFY_ATTEMPTS, "Max verification attempts",
                        "How many wrong guesses are allowed before the code is rejected.",
                        Integer.toString(ConfigKeys.DEFAULT_MAX_VERIFY_ATTEMPTS)),
                string(ConfigKeys.RESEND_COOLDOWN_SECONDS, "Resend cooldown (seconds)",
                        "Minimum delay between resend requests.",
                        Integer.toString(ConfigKeys.DEFAULT_RESEND_COOLDOWN_SECONDS)),
                string(ConfigKeys.MAX_RESENDS, "Max resends",
                        "How many times the user may request a new code per login attempt.",
                        Integer.toString(ConfigKeys.DEFAULT_MAX_RESENDS)),
                text(ConfigKeys.SMS_TEXT_TEMPLATE, "SMS text template",
                        "Message body. Placeholders: %code% and %ttl% (validity in minutes).",
                        ConfigKeys.DEFAULT_SMS_TEXT_TEMPLATE),

                // --- Phone number ---
                string(ConfigKeys.PHONE_NUMBER_ATTRIBUTE, "Phone number attribute",
                        "User attribute holding the phone number. Defaults to 'phoneNumber', which "
                                + "Keycloak's built-in OIDC 'phone' scope maps to the phone_number claim.",
                        ConfigKeys.DEFAULT_PHONE_NUMBER_ATTRIBUTE),
                string(ConfigKeys.PHONE_NUMBER_VERIFIED_ATTRIBUTE, "Phone verified attribute",
                        "User attribute set to true after a successful SMS verification; maps to the "
                                + "phone_number_verified claim.",
                        ConfigKeys.DEFAULT_PHONE_NUMBER_VERIFIED_ATTRIBUTE),
                bool(ConfigKeys.MARK_PHONE_VERIFIED, "Mark phone verified on success",
                        "When on, a successful OTP sets the phone-verified attribute to true.",
                        ConfigKeys.DEFAULT_MARK_PHONE_VERIFIED),
                string(ConfigKeys.DEFAULT_COUNTRY_CODE, "Default country code",
                        "Dialing code applied to numbers stored without an international prefix (e.g. +36).",
                        ConfigKeys.DEFAULT_COUNTRY_CODE_VALUE),

                // --- BulkGate gateway ---
                bool(ConfigKeys.SIMULATION_MODE, "Simulation mode",
                        "When on, codes are logged to the server instead of sent via BulkGate.",
                        ConfigKeys.DEFAULT_SIMULATION_MODE),
                string(ConfigKeys.BULKGATE_API_URL, "BulkGate API URL",
                        "Advanced transactional endpoint. Override only for testing/mocking.",
                        io.github.qcodr.keycloak.bulkgate.gateway.BulkGateSettings.DEFAULT_API_URL),
                string(ConfigKeys.BULKGATE_APPLICATION_ID, "BulkGate application id",
                        "The Application ID from your BulkGate API application.",
                        ""),
                token,
                string(ConfigKeys.BULKGATE_SENDER_ID, "Sender id type",
                        "BulkGate sender id type: gSystem, gText, gOwn, gProfile, gMobile, gPush.",
                        io.github.qcodr.keycloak.bulkgate.gateway.BulkGateSettings.DEFAULT_SENDER_ID),
                string(ConfigKeys.BULKGATE_SENDER_ID_VALUE, "Sender id value",
                        "Value for the chosen sender id type (e.g. a text sender name or phone number).",
                        ""),
                bool(ConfigKeys.BULKGATE_UNICODE, "Unicode",
                        "Send the message as Unicode (needed for non-GSM characters).",
                        false),
                string(ConfigKeys.BULKGATE_COUNTRY, "Country hint",
                        "Optional ISO country code passed to BulkGate for number detection.",
                        ""));
    }

    private static ProviderConfigProperty string(String name, String label, String help, String defaultValue) {
        return new ProviderConfigProperty(name, label, help, ProviderConfigProperty.STRING_TYPE, defaultValue);
    }

    private static ProviderConfigProperty text(String name, String label, String help, String defaultValue) {
        return new ProviderConfigProperty(name, label, help, ProviderConfigProperty.TEXT_TYPE, defaultValue);
    }

    private static ProviderConfigProperty bool(String name, String label, String help, boolean defaultValue) {
        return new ProviderConfigProperty(name, label, help, ProviderConfigProperty.BOOLEAN_TYPE, defaultValue);
    }
}
