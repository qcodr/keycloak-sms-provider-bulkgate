/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

/**
 * Produces one-time codes. An interface so the cryptographic source can be
 * swapped (e.g. a deterministic generator in tests) without touching callers.
 */
public interface OtpCodeGenerator {

    /**
     * @param length number of digits, must be positive
     * @return a numeric code of exactly {@code length} digits (leading zeros allowed)
     */
    OtpCode generate(int length);
}
