/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * Decides whether a submitted code satisfies a challenge. Pure and free of any
 * Keycloak dependency, so the security-critical rules (order of checks, expiry
 * before match, constant-time comparison) are exhaustively unit-testable.
 *
 * <p>Order matters: a missing challenge and expiry are checked before the code
 * is compared, and attempt exhaustion is checked before a fresh comparison, so
 * an attacker cannot keep guessing past the configured limit.</p>
 */
public class OtpVerifier {

    private final OtpHasher hasher;

    public OtpVerifier(OtpHasher hasher) {
        this.hasher = hasher;
    }

    public OtpVerificationResult verify(OtpChallenge challenge, String submittedCode, Instant now, int maxAttempts) {
        if (challenge == null) {
            return OtpVerificationResult.NO_CHALLENGE;
        }
        if (challenge.isExpiredAt(now)) {
            // Expiry is checked before the attempt budget on purpose: an expired code
            // can never verify, so submitting one does not consume an attempt.
            return OtpVerificationResult.EXPIRED;
        }
        if (challenge.attempts() >= maxAttempts) {
            return OtpVerificationResult.TOO_MANY_ATTEMPTS;
        }
        if (submittedCode == null) {
            return OtpVerificationResult.INVALID;
        }
        String submittedHash = hasher.hash(submittedCode, challenge.salt());
        return constantTimeEquals(submittedHash, challenge.hashedCode())
                ? OtpVerificationResult.VALID
                : OtpVerificationResult.INVALID;
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
