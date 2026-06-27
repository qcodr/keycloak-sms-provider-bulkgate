/* Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0. */
package io.github.qcodr.keycloak.bulkgate.otp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class SecureRandomSaltSourceTest {

    private final SaltSource saltSource = new SecureRandomSaltSource();

    @Test
    void producesNonBlankUrlSafeBase64() {
        String salt = saltSource.newSalt();

        assertThat(salt).isNotBlank();
        // URL-safe, no padding -> must decode and must not contain +, /, or =.
        assertThat(salt).doesNotContain("+", "/", "=");
        assertThat(Base64.getUrlDecoder().decode(salt)).hasSize(16);
    }

    @RepeatedTest(20)
    void generatesDistinctSaltsAcrossCalls() {
        assertThat(saltSource.newSalt()).isNotEqualTo(saltSource.newSalt());
    }

    @Test
    void isOverwhelminglyUniqueOverManyCalls() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            seen.add(saltSource.newSalt());
        }
        assertThat(seen).hasSize(1000);
    }
}
