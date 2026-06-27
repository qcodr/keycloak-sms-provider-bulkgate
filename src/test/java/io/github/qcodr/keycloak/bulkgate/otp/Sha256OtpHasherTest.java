/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Sha256OtpHasherTest {

    private final OtpHasher hasher = new Sha256OtpHasher();

    @Test
    void isDeterministicForSameInputs() {
        assertThat(hasher.hash("123456", "salt")).isEqualTo(hasher.hash("123456", "salt"));
    }

    @Test
    void differsWhenSaltDiffers() {
        assertThat(hasher.hash("123456", "saltA")).isNotEqualTo(hasher.hash("123456", "saltB"));
    }

    @Test
    void differsWhenCodeDiffers() {
        assertThat(hasher.hash("123456", "salt")).isNotEqualTo(hasher.hash("654321", "salt"));
    }

    @Test
    void doesNotLeakPlaintextCode() {
        String digest = hasher.hash("123456", "salt");

        assertThat(digest).doesNotContain("123456");
    }

    @Test
    void producesLowercaseHexOfExpectedLength() {
        // SHA-256 -> 32 bytes -> 64 hex chars.
        assertThat(hasher.hash("123456", "salt")).hasSize(64).matches("[0-9a-f]{64}");
    }
}
