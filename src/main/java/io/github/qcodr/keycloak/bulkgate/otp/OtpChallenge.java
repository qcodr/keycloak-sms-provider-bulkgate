/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

import java.time.Instant;
import java.util.Objects;

/**
 * The server-side state of one outstanding OTP, as stored in the authentication
 * session. Holds only the hash of the code — never the code — plus the metadata
 * needed to enforce expiry, attempt limits, and resend throttling.
 *
 * <p>Immutable: state transitions return a new instance.</p>
 */
public record OtpChallenge(
        String hashedCode,
        String salt,
        Instant expiresAt,
        int attempts,
        int resends,
        String recipient,
        Instant lastSentAt) {

    public OtpChallenge {
        Objects.requireNonNull(hashedCode, "hashedCode");
        Objects.requireNonNull(salt, "salt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(recipient, "recipient");
        Objects.requireNonNull(lastSentAt, "lastSentAt");
        if (attempts < 0 || resends < 0) {
            throw new IllegalArgumentException("attempts and resends must not be negative");
        }
    }

    /** @return a copy with the failed-attempt counter increased by one */
    public OtpChallenge withIncrementedAttempts() {
        return new OtpChallenge(hashedCode, salt, expiresAt, attempts + 1, resends, recipient, lastSentAt);
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
