/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

/**
 * Supplies fresh random salts for OTP hashing. An interface so tests can pin the
 * salt and assert on exact hashes.
 */
@FunctionalInterface
public interface SaltSource {

    String newSalt();
}
