/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 implementation of {@link OtpHasher}.
 *
 * <p>A short numeric OTP has tiny entropy, so a slow password hash buys little
 * here; what matters is that the plaintext is not stored and that the salt makes
 * the stored digest specific to one challenge. SHA-256 over {@code salt:code}
 * meets that bar and has no external dependency.</p>
 */
public class Sha256OtpHasher implements OtpHasher {

    private static final String ALGORITHM = "SHA-256";

    @Override
    public String hash(String code, String salt) {
        if (code == null || salt == null) {
            throw new IllegalArgumentException("code and salt must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            byte[] hashed = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JLS for every JVM; this cannot happen.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
