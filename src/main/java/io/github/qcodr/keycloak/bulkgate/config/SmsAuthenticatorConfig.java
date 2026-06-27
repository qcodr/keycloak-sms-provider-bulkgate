/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.config;

import io.github.qcodr.keycloak.bulkgate.gateway.BulkGateSettings;
import java.time.Duration;
import java.util.Map;

/**
 * Immutable, validated view over the raw per-realm configuration map that
 * Keycloak hands to the authenticator.
 *
 * <p>Parsing and validation live here, once, so the rest of the code works with
 * typed values and never re-parses strings or guesses at defaults.</p>
 */
public record SmsAuthenticatorConfig(
        int codeLength,
        Duration codeTtl,
        int maxVerifyAttempts,
        Duration resendCooldown,
        int maxResends,
        String phoneNumberAttribute,
        String phoneNumberVerifiedAttribute,
        boolean markPhoneVerified,
        String defaultCountryCode,
        boolean simulationMode,
        String smsTextTemplate,
        BulkGateSettings bulkGate) {

    /**
     * Builds a config from Keycloak's raw map, applying defaults for missing keys
     * and validating bounds.
     *
     * @param raw the {@code AuthenticatorConfigModel} config map (may be {@code null})
     * @throws InvalidConfigurationException when a present value is out of range
     */
    public static SmsAuthenticatorConfig from(Map<String, String> raw) {
        Map<String, String> config = raw == null ? Map.of() : raw;

        int codeLength = intValue(config, ConfigKeys.CODE_LENGTH, ConfigKeys.DEFAULT_CODE_LENGTH);
        if (codeLength < ConfigKeys.MIN_CODE_LENGTH || codeLength > ConfigKeys.MAX_CODE_LENGTH) {
            throw new InvalidConfigurationException("codeLength must be between " + ConfigKeys.MIN_CODE_LENGTH + " and "
                    + ConfigKeys.MAX_CODE_LENGTH + ", was " + codeLength);
        }

        int ttlSeconds = positive(config, ConfigKeys.CODE_TTL_SECONDS, ConfigKeys.DEFAULT_CODE_TTL_SECONDS);
        int maxVerifyAttempts =
                positive(config, ConfigKeys.MAX_VERIFY_ATTEMPTS, ConfigKeys.DEFAULT_MAX_VERIFY_ATTEMPTS);
        int resendCooldown =
                nonNegative(config, ConfigKeys.RESEND_COOLDOWN_SECONDS, ConfigKeys.DEFAULT_RESEND_COOLDOWN_SECONDS);
        int maxResends = nonNegative(config, ConfigKeys.MAX_RESENDS, ConfigKeys.DEFAULT_MAX_RESENDS);

        String phoneAttr =
                stringValue(config, ConfigKeys.PHONE_NUMBER_ATTRIBUTE, ConfigKeys.DEFAULT_PHONE_NUMBER_ATTRIBUTE);
        String phoneVerifiedAttr = stringValue(
                config, ConfigKeys.PHONE_NUMBER_VERIFIED_ATTRIBUTE, ConfigKeys.DEFAULT_PHONE_NUMBER_VERIFIED_ATTRIBUTE);
        boolean markVerified =
                boolValue(config, ConfigKeys.MARK_PHONE_VERIFIED, ConfigKeys.DEFAULT_MARK_PHONE_VERIFIED);
        String countryCode =
                stringValue(config, ConfigKeys.DEFAULT_COUNTRY_CODE, ConfigKeys.DEFAULT_COUNTRY_CODE_VALUE);
        boolean simulation = boolValue(config, ConfigKeys.SIMULATION_MODE, ConfigKeys.DEFAULT_SIMULATION_MODE);
        String template = stringValue(config, ConfigKeys.SMS_TEXT_TEMPLATE, ConfigKeys.DEFAULT_SMS_TEXT_TEMPLATE);

        BulkGateSettings bulkGate = new BulkGateSettings(
                stringValue(config, ConfigKeys.BULKGATE_API_URL, BulkGateSettings.DEFAULT_API_URL),
                config.get(ConfigKeys.BULKGATE_APPLICATION_ID),
                config.get(ConfigKeys.BULKGATE_APPLICATION_TOKEN),
                stringValue(config, ConfigKeys.BULKGATE_SENDER_ID, BulkGateSettings.DEFAULT_SENDER_ID),
                config.get(ConfigKeys.BULKGATE_SENDER_ID_VALUE),
                boolValue(config, ConfigKeys.BULKGATE_UNICODE, false),
                config.get(ConfigKeys.BULKGATE_COUNTRY));
        requireHttpUrl(bulkGate.apiUrl());

        return new SmsAuthenticatorConfig(
                codeLength,
                Duration.ofSeconds(ttlSeconds),
                maxVerifyAttempts,
                Duration.ofSeconds(resendCooldown),
                maxResends,
                phoneAttr,
                phoneVerifiedAttr,
                markVerified,
                countryCode,
                simulation,
                template,
                bulkGate);
    }

    /** TTL rounded up to whole minutes, for human-facing SMS text (never zero). */
    public long ttlMinutes() {
        return Math.max(1, (codeTtl.getSeconds() + 59) / 60);
    }

    /**
     * Restricts the BulkGate endpoint to {@code http}/{@code https}. The URL is
     * admin-supplied, so this is a guard against accidental or hostile schemes
     * (e.g. {@code file:}) being turned into outbound requests.
     */
    private static void requireHttpUrl(String apiUrl) {
        String scheme;
        try {
            scheme = java.net.URI.create(apiUrl).getScheme();
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigurationException("bulkgateApiUrl is not a valid URL: " + apiUrl);
        }
        if (!isHttpOrHttps(scheme)) {
            throw new InvalidConfigurationException("bulkgateApiUrl must be http or https, was: " + apiUrl);
        }
    }

    private static boolean isHttpOrHttps(String scheme) {
        return equalsAsciiIgnoreCase(scheme, "http") || equalsAsciiIgnoreCase(scheme, "https");
    }

    /** ASCII-only case-insensitive equality, avoiding locale/Unicode case-folding surprises. */
    private static boolean equalsAsciiIgnoreCase(String value, String asciiLower) {
        if (value == null || value.length() != asciiLower.length()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c += 32;
            }
            if (c != asciiLower.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static String stringValue(Map<String, String> config, String key, String fallback) {
        String value = config.get(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static boolean boolValue(Map<String, String> config, String key, boolean fallback) {
        String value = config.get(key);
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static int intValue(Map<String, String> config, String key, int fallback) {
        String value = config.get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidConfigurationException(key + " must be an integer, was '" + value + "'");
        }
    }

    private static int positive(Map<String, String> config, String key, int fallback) {
        int value = intValue(config, key, fallback);
        if (value <= 0) {
            throw new InvalidConfigurationException(key + " must be greater than 0, was " + value);
        }
        return value;
    }

    private static int nonNegative(Map<String, String> config, String key, int fallback) {
        int value = intValue(config, key, fallback);
        if (value < 0) {
            throw new InvalidConfigurationException(key + " must not be negative, was " + value);
        }
        return value;
    }
}
