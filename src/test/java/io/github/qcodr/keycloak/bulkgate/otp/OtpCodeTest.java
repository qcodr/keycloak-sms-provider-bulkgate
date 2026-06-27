/* Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0. */
package io.github.qcodr.keycloak.bulkgate.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OtpCodeTest {

    @Test
    void exposesValueAndLength() {
        OtpCode code = new OtpCode("123456");

        assertThat(code.value()).isEqualTo("123456");
        assertThat(code.length()).isEqualTo(6);
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> new OtpCode(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankValue() {
        assertThatThrownBy(() -> new OtpCode("   ")).isInstanceOf(IllegalArgumentException.class);
    }
}
