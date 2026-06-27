/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.config;

/**
 * Single source of truth for the per-realm authenticator configuration keys and
 * their defaults. The factory exposes these as admin-console fields; the
 * {@link SmsAuthenticatorConfig} reads them back. Keeping the literals here
 * prevents the two sides from silently drifting apart.
 */
public final class ConfigKeys {

    private ConfigKeys() {
    }

    // --- OTP behaviour ------------------------------------------------------
    public static final String CODE_LENGTH = "codeLength";
    public static final String CODE_TTL_SECONDS = "codeTtlSeconds";
    public static final String MAX_VERIFY_ATTEMPTS = "maxVerifyAttempts";
    public static final String RESEND_COOLDOWN_SECONDS = "resendCooldownSeconds";
    public static final String MAX_RESENDS = "maxResends";
    public static final String SMS_TEXT_TEMPLATE = "smsTextTemplate";

    // --- Phone number -------------------------------------------------------
    public static final String PHONE_NUMBER_ATTRIBUTE = "phoneNumberAttribute";
    public static final String DEFAULT_COUNTRY_CODE = "defaultCountryCode";

    // --- BulkGate gateway ---------------------------------------------------
    public static final String SIMULATION_MODE = "simulationMode";
    public static final String BULKGATE_API_URL = "bulkgateApiUrl";
    public static final String BULKGATE_APPLICATION_ID = "bulkgateApplicationId";
    public static final String BULKGATE_APPLICATION_TOKEN = "bulkgateApplicationToken";
    public static final String BULKGATE_SENDER_ID = "bulkgateSenderId";
    public static final String BULKGATE_SENDER_ID_VALUE = "bulkgateSenderIdValue";
    public static final String BULKGATE_UNICODE = "bulkgateUnicode";
    public static final String BULKGATE_COUNTRY = "bulkgateCountry";

    // --- Defaults -----------------------------------------------------------
    public static final int DEFAULT_CODE_LENGTH = 6;
    public static final int DEFAULT_CODE_TTL_SECONDS = 300;
    public static final int DEFAULT_MAX_VERIFY_ATTEMPTS = 3;
    public static final int DEFAULT_RESEND_COOLDOWN_SECONDS = 30;
    public static final int DEFAULT_MAX_RESENDS = 3;
    public static final String DEFAULT_PHONE_NUMBER_ATTRIBUTE = "mobile_number";
    public static final String DEFAULT_COUNTRY_CODE_VALUE = "+36";
    public static final boolean DEFAULT_SIMULATION_MODE = false;
    public static final String DEFAULT_SMS_TEXT_TEMPLATE =
            "Your verification code is %code%. It is valid for %ttl% minutes.";

    // --- Validation bounds --------------------------------------------------
    public static final int MIN_CODE_LENGTH = 4;
    public static final int MAX_CODE_LENGTH = 10;
}
