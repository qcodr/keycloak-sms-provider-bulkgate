/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

/**
 * Hashes a plaintext code with a salt so that only the digest is ever stored in
 * the (browser-readable) authentication session, never the code itself.
 */
public interface OtpHasher {

    /**
     * @param code plaintext code
     * @param salt per-challenge random salt
     * @return a deterministic, encoded digest of {@code salt} + {@code code}
     */
    String hash(String code, String salt);
}
