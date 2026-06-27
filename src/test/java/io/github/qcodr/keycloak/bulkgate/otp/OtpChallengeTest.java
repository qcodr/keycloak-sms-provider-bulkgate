/* Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0. */
package io.github.qcodr.keycloak.bulkgate.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OtpChallengeTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    private OtpChallenge challenge(int attempts, int resends) {
        return new OtpChallenge("hash", "salt", NOW.plusSeconds(300), attempts, resends, "+36201234567", NOW);
    }

    @Test
    void rejectsNegativeAttemptsOrResends() {
        assertThatThrownBy(() -> challenge(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> challenge(0, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThatThrownBy(() -> new OtpChallenge(null, "s", NOW, 0, 0, "+3620", NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OtpChallenge("h", "s", NOW, 0, 0, null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void incrementsAttemptsImmutably() {
        OtpChallenge original = challenge(1, 0);

        OtpChallenge next = original.withIncrementedAttempts();

        assertThat(next.attempts()).isEqualTo(2);
        assertThat(original.attempts()).isEqualTo(1); // unchanged
        assertThat(next.hashedCode()).isEqualTo(original.hashedCode());
    }

    @Test
    void isExpiredOnlyAtOrAfterExpiry() {
        OtpChallenge c = challenge(0, 0); // expires at NOW+300s

        assertThat(c.isExpiredAt(NOW.plusSeconds(299))).isFalse();
        assertThat(c.isExpiredAt(NOW.plusSeconds(300))).isTrue();
        assertThat(c.isExpiredAt(NOW.plusSeconds(301))).isTrue();
    }
}
