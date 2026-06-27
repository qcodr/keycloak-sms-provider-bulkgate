/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Default {@link SaltSource}: 16 cryptographically random bytes, Base64 (URL,
 * no padding) encoded so the salt is safe to store verbatim in a session note.
 */
public class SecureRandomSaltSource implements SaltSource {

    private static final int SALT_BYTES = 16;

    private final SecureRandom random;

    public SecureRandomSaltSource() {
        this(new SecureRandom());
    }

    public SecureRandomSaltSource(SecureRandom random) {
        this.random = random;
    }

    @Override
    public String newSalt() {
        byte[] bytes = new byte[SALT_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
