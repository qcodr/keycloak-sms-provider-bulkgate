/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.phone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LibPhoneNumberNormalizerTest {

    private final PhoneNumberNormalizer hungarian = new LibPhoneNumberNormalizer("+36");

    @ParameterizedTest
    @CsvSource({
            "+36201234567,      +36201234567",
            "0036201234567,     +36201234567",
            "06201234567,       +36201234567",   // Hungarian two-digit trunk prefix '06'
            "+36 20 123 4567,   +36201234567",
            "+36-20-123-4567,   +36201234567",
            "(+36) 20/123.4567, +36201234567"
    })
    void normalizesCommonHungarianNotationsToE164(String raw, String expected) {
        assertThat(hungarian.normalize(raw).e164()).isEqualTo(expected);
    }

    @Test
    void handlesAnotherRegionsTrunkPrefix() {
        // UK: single-digit trunk '0', different rules from Hungary's '06'.
        PhoneNumberNormalizer uk = new LibPhoneNumberNormalizer("+44");

        assertThat(uk.normalize("07400123456").e164()).isEqualTo("+447400123456");
    }

    @Test
    void acceptsDialingCodeWithoutPlus() {
        PhoneNumberNormalizer uk = new LibPhoneNumberNormalizer("44");

        assertThat(uk.normalize("07400123456").e164()).isEqualTo("+447400123456");
    }

    @Test
    void exposesNumberWithoutPlusForApisThatNeedIt() {
        assertThat(hungarian.normalize("+36201234567").withoutPlus()).isEqualTo("36201234567");
    }

    @Test
    void rejectsBlankInput() {
        assertThatThrownBy(() -> hungarian.normalize("   "))
                .isInstanceOf(InvalidPhoneNumberException.class);
    }

    @Test
    void rejectsUnparseableInput() {
        assertThatThrownBy(() -> hungarian.normalize("not-a-number"))
                .isInstanceOf(InvalidPhoneNumberException.class);
    }

    @Test
    void rejectsTooShortNumber() {
        assertThatThrownBy(() -> hungarian.normalize("12"))
                .isInstanceOf(InvalidPhoneNumberException.class);
    }
}
