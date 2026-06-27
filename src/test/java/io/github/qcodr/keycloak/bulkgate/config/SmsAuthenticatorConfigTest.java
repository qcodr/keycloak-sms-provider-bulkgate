/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmsAuthenticatorConfigTest {

    @Test
    void appliesDefaultsForEmptyConfig() {
        SmsAuthenticatorConfig config = SmsAuthenticatorConfig.from(Map.of());

        assertThat(config.codeLength()).isEqualTo(ConfigKeys.DEFAULT_CODE_LENGTH);
        assertThat(config.codeTtl()).isEqualTo(Duration.ofSeconds(ConfigKeys.DEFAULT_CODE_TTL_SECONDS));
        assertThat(config.maxVerifyAttempts()).isEqualTo(ConfigKeys.DEFAULT_MAX_VERIFY_ATTEMPTS);
        assertThat(config.phoneNumberAttribute()).isEqualTo(ConfigKeys.DEFAULT_PHONE_NUMBER_ATTRIBUTE);
        assertThat(config.defaultCountryCode()).isEqualTo(ConfigKeys.DEFAULT_COUNTRY_CODE_VALUE);
        assertThat(config.simulationMode()).isFalse();
        assertThat(config.bulkGate().apiUrl()).isEqualTo("https://portal.bulkgate.com/api/1.0/advanced/transactional");
    }

    @Test
    void toleratesNullConfigMap() {
        SmsAuthenticatorConfig config = SmsAuthenticatorConfig.from(null);

        assertThat(config.codeLength()).isEqualTo(ConfigKeys.DEFAULT_CODE_LENGTH);
    }

    @Test
    void readsExplicitValues() {
        Map<String, String> raw = new HashMap<>();
        raw.put(ConfigKeys.CODE_LENGTH, "8");
        raw.put(ConfigKeys.CODE_TTL_SECONDS, "120");
        raw.put(ConfigKeys.MAX_VERIFY_ATTEMPTS, "5");
        raw.put(ConfigKeys.RESEND_COOLDOWN_SECONDS, "15");
        raw.put(ConfigKeys.MAX_RESENDS, "2");
        raw.put(ConfigKeys.PHONE_NUMBER_ATTRIBUTE, "phone");
        raw.put(ConfigKeys.DEFAULT_COUNTRY_CODE, "+49");
        raw.put(ConfigKeys.SIMULATION_MODE, "true");
        raw.put(ConfigKeys.BULKGATE_APPLICATION_ID, "app-1");
        raw.put(ConfigKeys.BULKGATE_APPLICATION_TOKEN, "token-1");
        raw.put(ConfigKeys.BULKGATE_SENDER_ID, "gText");
        raw.put(ConfigKeys.BULKGATE_SENDER_ID_VALUE, "Keycloak");

        SmsAuthenticatorConfig config = SmsAuthenticatorConfig.from(raw);

        assertThat(config.codeLength()).isEqualTo(8);
        assertThat(config.codeTtl()).isEqualTo(Duration.ofSeconds(120));
        assertThat(config.maxVerifyAttempts()).isEqualTo(5);
        assertThat(config.resendCooldown()).isEqualTo(Duration.ofSeconds(15));
        assertThat(config.maxResends()).isEqualTo(2);
        assertThat(config.phoneNumberAttribute()).isEqualTo("phone");
        assertThat(config.defaultCountryCode()).isEqualTo("+49");
        assertThat(config.simulationMode()).isTrue();
        assertThat(config.bulkGate().applicationId()).isEqualTo("app-1");
        assertThat(config.bulkGate().senderId()).isEqualTo("gText");
        assertThat(config.bulkGate().senderIdValue()).isEqualTo("Keycloak");
    }

    @Test
    void ttlMinutesRoundsUp() {
        SmsAuthenticatorConfig config = SmsAuthenticatorConfig.from(Map.of(ConfigKeys.CODE_TTL_SECONDS, "90"));

        assertThat(config.ttlMinutes()).isEqualTo(2);
    }

    @Test
    void rejectsCodeLengthBelowMinimum() {
        assertThatThrownBy(() -> SmsAuthenticatorConfig.from(Map.of(ConfigKeys.CODE_LENGTH, "3")))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void rejectsCodeLengthAboveMaximum() {
        assertThatThrownBy(() -> SmsAuthenticatorConfig.from(Map.of(ConfigKeys.CODE_LENGTH, "11")))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void rejectsNonNumericInteger() {
        assertThatThrownBy(() -> SmsAuthenticatorConfig.from(Map.of(ConfigKeys.CODE_TTL_SECONDS, "abc")))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void rejectsNonPositiveTtl() {
        assertThatThrownBy(() -> SmsAuthenticatorConfig.from(Map.of(ConfigKeys.CODE_TTL_SECONDS, "0")))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void rejectsNonHttpApiUrl() {
        assertThatThrownBy(() -> SmsAuthenticatorConfig.from(Map.of(ConfigKeys.BULKGATE_API_URL, "file:///etc/passwd")))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void allowsHttpApiUrlForMocking() {
        SmsAuthenticatorConfig config =
                SmsAuthenticatorConfig.from(Map.of(ConfigKeys.BULKGATE_API_URL, "http://wiremock:8080/api"));

        assertThat(config.bulkGate().apiUrl()).isEqualTo("http://wiremock:8080/api");
    }
}
