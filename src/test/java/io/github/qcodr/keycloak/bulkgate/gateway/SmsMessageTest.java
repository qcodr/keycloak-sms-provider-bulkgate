/* Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0. */
package io.github.qcodr.keycloak.bulkgate.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SmsMessageTest {

    @Test
    void keepsRecipientAndText() {
        SmsMessage message = new SmsMessage("+36201234567", "hello");

        assertThat(message.recipient()).isEqualTo("+36201234567");
        assertThat(message.text()).isEqualTo("hello");
    }

    @Test
    void rejectsNullRecipientOrText() {
        assertThatThrownBy(() -> new SmsMessage(null, "x")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SmsMessage("+3620", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsBlankRecipientOrText() {
        assertThatThrownBy(() -> new SmsMessage("  ", "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SmsMessage("+3620", "  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
