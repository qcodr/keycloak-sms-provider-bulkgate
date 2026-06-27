/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

import java.security.SecureRandom;

/**
 * Generates numeric OTP codes from a {@link SecureRandom} source.
 *
 * <p>Each digit is drawn independently and uniformly from 0-9, so there is no
 * modulo bias and leading zeros occur naturally.</p>
 */
public class SecureRandomOtpCodeGenerator implements OtpCodeGenerator {

    private final SecureRandom random;

    public SecureRandomOtpCodeGenerator() {
        this(new SecureRandom());
    }

    public SecureRandomOtpCodeGenerator(SecureRandom random) {
        this.random = random;
    }

    @Override
    public OtpCode generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive, was " + length);
        }
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(random.nextInt(10));
        }
        return new OtpCode(builder.toString());
    }
}
