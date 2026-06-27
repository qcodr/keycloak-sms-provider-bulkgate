/* Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0. */
package io.github.qcodr.keycloak.bulkgate.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BulkGateSettingsTest {

    private BulkGateSettings settings(String appId, String token, String senderIdValue, String country) {
        return new BulkGateSettings(null, appId, token, null, senderIdValue, false, country);
    }

    @Test
    void appliesDefaultsForBlankUrlAndSenderId() {
        BulkGateSettings s = settings("app", "tok", "", "");

        assertThat(s.apiUrl()).isEqualTo(BulkGateSettings.DEFAULT_API_URL);
        assertThat(s.senderId()).isEqualTo(BulkGateSettings.DEFAULT_SENDER_ID);
    }

    @Test
    void requireCredentialsPassesWhenBothPresent() {
        settings("app", "tok", "", "").requireCredentials(); // no throw
    }

    @Test
    void requireCredentialsFailsWhenIdMissing() {
        assertThatThrownBy(() -> settings("", "tok", "", "").requireCredentials())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requireCredentialsFailsWhenTokenMissing() {
        assertThatThrownBy(() -> settings("app", "", "", "").requireCredentials())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reportsPresenceOfOptionalSenderValueAndCountry() {
        assertThat(settings("app", "tok", "Brand", "HU").hasSenderIdValue()).isTrue();
        assertThat(settings("app", "tok", "Brand", "HU").hasCountry()).isTrue();
        assertThat(settings("app", "tok", "", "").hasSenderIdValue()).isFalse();
        assertThat(settings("app", "tok", "", "").hasCountry()).isFalse();
    }

    @Test
    void toStringNeverLeaksTheToken() {
        String text = settings("app", "super-secret-token", "", "").toString();

        assertThat(text).doesNotContain("super-secret-token").contains("***");
    }
}
