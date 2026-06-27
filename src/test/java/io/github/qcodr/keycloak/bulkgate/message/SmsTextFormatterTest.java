/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmsTextFormatterTest {

    private final SmsTextFormatter formatter = new SmsTextFormatter();

    @Test
    void substitutesCodeAndTtlPlaceholders() {
        String text = formatter.format("Code %code%, valid %ttl% min", "123456", 5);

        assertThat(text).isEqualTo("Code 123456, valid 5 min");
    }

    @Test
    void substitutesRepeatedPlaceholders() {
        String text = formatter.format("%code% %code%", "999", 1);

        assertThat(text).isEqualTo("999 999");
    }

    @Test
    void leavesTextWithoutPlaceholdersUntouched() {
        assertThat(formatter.format("No placeholders here", "123456", 5))
                .isEqualTo("No placeholders here");
    }

    @Test
    void rejectsBlankTemplate() {
        assertThatThrownBy(() -> formatter.format("  ", "123456", 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
