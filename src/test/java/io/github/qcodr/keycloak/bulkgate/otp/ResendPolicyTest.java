/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ResendPolicyTest {

    private static final Instant SENT_AT = Instant.parse("2026-01-01T12:00:00Z");
    private static final Duration COOLDOWN = Duration.ofSeconds(30);

    private OtpChallenge challengeWithResends(int resends) {
        return new OtpChallenge("hash", "salt", SENT_AT.plusSeconds(300), 0, resends, "+36201234567", SENT_AT);
    }

    @Test
    void allowsAfterCooldownAndUnderLimit() {
        OtpChallenge challenge = challengeWithResends(1);

        ResendPolicy.Decision decision =
                ResendPolicy.evaluate(challenge, 3, COOLDOWN, SENT_AT.plusSeconds(30));

        assertThat(decision).isEqualTo(ResendPolicy.Decision.ALLOWED);
    }

    @Test
    void blocksDuringCooldown() {
        OtpChallenge challenge = challengeWithResends(1);

        ResendPolicy.Decision decision =
                ResendPolicy.evaluate(challenge, 3, COOLDOWN, SENT_AT.plusSeconds(29));

        assertThat(decision).isEqualTo(ResendPolicy.Decision.COOLDOWN);
    }

    @Test
    void blocksWhenResendLimitReached() {
        OtpChallenge challenge = challengeWithResends(3);

        ResendPolicy.Decision decision =
                ResendPolicy.evaluate(challenge, 3, COOLDOWN, SENT_AT.plusSeconds(120));

        assertThat(decision).isEqualTo(ResendPolicy.Decision.LIMIT_REACHED);
    }

    @Test
    void limitTakesPrecedenceOverCooldown() {
        OtpChallenge challenge = challengeWithResends(3);

        ResendPolicy.Decision decision =
                ResendPolicy.evaluate(challenge, 3, COOLDOWN, SENT_AT.plusSeconds(1));

        assertThat(decision).isEqualTo(ResendPolicy.Decision.LIMIT_REACHED);
    }
}
