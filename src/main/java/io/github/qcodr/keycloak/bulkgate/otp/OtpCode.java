/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

/**
 * A freshly generated one-time code in plaintext. Lives only long enough to be
 * placed into an SMS; it is never persisted (only its hash is).
 */
public record OtpCode(String value) {

    public OtpCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OTP code must not be blank");
        }
    }

    public int length() {
        return value.length();
    }
}
