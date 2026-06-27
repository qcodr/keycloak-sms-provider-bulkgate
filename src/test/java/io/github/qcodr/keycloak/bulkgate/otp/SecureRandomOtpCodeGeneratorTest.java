/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class SecureRandomOtpCodeGeneratorTest {

    private final OtpCodeGenerator generator = new SecureRandomOtpCodeGenerator();

    @RepeatedTest(50)
    void generatesCodeOfRequestedLengthContainingOnlyDigits() {
        OtpCode code = generator.generate(6);

        assertThat(code.value()).hasSize(6).containsPattern("^\\d{6}$");
    }

    @Test
    void supportsConfigurableLength() {
        assertThat(generator.generate(8).value()).hasSize(8);
        assertThat(generator.generate(4).value()).hasSize(4);
    }

    @Test
    void rejectsNonPositiveLength() {
        assertThatThrownBy(() -> generator.generate(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
