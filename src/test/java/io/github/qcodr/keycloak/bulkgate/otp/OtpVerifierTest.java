/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OtpVerifierTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final String SALT = "fixed-salt";

    private final OtpHasher hasher = new Sha256OtpHasher();
    private final OtpVerifier verifier = new OtpVerifier(hasher);

    private OtpChallenge challengeFor(String code, Instant expiresAt, int attempts) {
        return new OtpChallenge(hasher.hash(code, SALT), SALT, expiresAt, attempts, 0, "+36201234567", NOW);
    }

    @Test
    void returnsValidWhenCodeMatchesAndFresh() {
        OtpChallenge challenge = challengeFor("123456", NOW.plus(Duration.ofMinutes(5)), 0);

        assertThat(verifier.verify(challenge, "123456", NOW, 3)).isEqualTo(OtpVerificationResult.VALID);
    }

    @Test
    void returnsInvalidWhenCodeDoesNotMatch() {
        OtpChallenge challenge = challengeFor("123456", NOW.plus(Duration.ofMinutes(5)), 0);

        assertThat(verifier.verify(challenge, "000000", NOW, 3)).isEqualTo(OtpVerificationResult.INVALID);
    }

    @Test
    void returnsExpiredWhenTtlElapsedEvenIfCodeMatches() {
        OtpChallenge challenge = challengeFor("123456", NOW.minusSeconds(1), 0);

        assertThat(verifier.verify(challenge, "123456", NOW, 3)).isEqualTo(OtpVerificationResult.EXPIRED);
    }

    @Test
    void returnsTooManyAttemptsWhenBudgetExhausted() {
        OtpChallenge challenge = challengeFor("123456", NOW.plus(Duration.ofMinutes(5)), 3);

        assertThat(verifier.verify(challenge, "123456", NOW, 3)).isEqualTo(OtpVerificationResult.TOO_MANY_ATTEMPTS);
    }

    @Test
    void returnsNoChallengeWhenNull() {
        assertThat(verifier.verify(null, "123456", NOW, 3)).isEqualTo(OtpVerificationResult.NO_CHALLENGE);
    }

    @Test
    void returnsInvalidWhenSubmittedCodeIsNull() {
        OtpChallenge challenge = challengeFor("123456", NOW.plus(Duration.ofMinutes(5)), 0);

        assertThat(verifier.verify(challenge, null, NOW, 3)).isEqualTo(OtpVerificationResult.INVALID);
    }

    @Test
    void checksExpiryBeforeAttemptBudget() {
        // Expired AND out of attempts -> expiry wins (checked first).
        OtpChallenge challenge = challengeFor("123456", NOW.minusSeconds(1), 99);

        assertThat(verifier.verify(challenge, "123456", NOW, 3)).isEqualTo(OtpVerificationResult.EXPIRED);
    }
}
