/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class OtpChallengeFactoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    private final OtpHasher hasher = new Sha256OtpHasher();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private OtpChallengeFactory factoryEmitting(String fixedCode, String fixedSalt) {
        OtpCodeGenerator generator = length -> new OtpCode(fixedCode);
        SaltSource saltSource = () -> fixedSalt;
        return new OtpChallengeFactory(generator, hasher, saltSource, clock);
    }

    @Test
    void issuesPlaintextSeparateFromStoredHash() {
        OtpChallengeFactory factory = factoryEmitting("123456", "salt-1");

        IssuedOtp issued = factory.issue(6, Duration.ofMinutes(5), "+36201234567", 0, 0);

        assertThat(issued.plaintext().value()).isEqualTo("123456");
        assertThat(issued.challenge().hashedCode()).isEqualTo(hasher.hash("123456", "salt-1"));
        assertThat(issued.challenge().hashedCode()).doesNotContain("123456");
    }

    @Test
    void stampsExpiryRelativeToClock() {
        OtpChallengeFactory factory = factoryEmitting("123456", "salt-1");

        IssuedOtp issued = factory.issue(6, Duration.ofSeconds(300), "+36201234567", 0, 0);

        assertThat(issued.challenge().expiresAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(issued.challenge().lastSentAt()).isEqualTo(NOW);
        assertThat(issued.challenge().attempts()).isZero();
    }

    @Test
    void carriesForwardPriorResendCount() {
        OtpChallengeFactory factory = factoryEmitting("123456", "salt-1");

        IssuedOtp issued = factory.issue(6, Duration.ofMinutes(5), "+36201234567", 2, 0);

        assertThat(issued.challenge().resends()).isEqualTo(2);
    }

    @Test
    void carriesForwardPriorAttemptsSoResendDoesNotResetBudget() {
        OtpChallengeFactory factory = factoryEmitting("123456", "salt-1");

        IssuedOtp issued = factory.issue(6, Duration.ofMinutes(5), "+36201234567", 1, 3);

        assertThat(issued.challenge().attempts()).isEqualTo(3);
    }
}
