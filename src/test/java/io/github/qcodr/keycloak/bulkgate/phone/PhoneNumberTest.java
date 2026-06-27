/* Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0. */
package io.github.qcodr.keycloak.bulkgate.phone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PhoneNumberTest {

    @Test
    void acceptsValidE164AndStripsPlus() {
        PhoneNumber number = new PhoneNumber("+36201234567");

        assertThat(number.e164()).isEqualTo("+36201234567");
        assertThat(number.withoutPlus()).isEqualTo("36201234567");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new PhoneNumber(null)).isInstanceOf(InvalidPhoneNumberException.class);
    }

    @Test
    void rejectsNonE164Strings() {
        assertThatThrownBy(() -> new PhoneNumber("0036201234567")).isInstanceOf(InvalidPhoneNumberException.class);
        assertThatThrownBy(() -> new PhoneNumber("+abc")).isInstanceOf(InvalidPhoneNumberException.class);
        assertThatThrownBy(() -> new PhoneNumber("12345")).isInstanceOf(InvalidPhoneNumberException.class);
    }
}
