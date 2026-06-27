/* Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0. */
package io.github.qcodr.keycloak.bulkgate.requiredaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNumberRequiredActionFactoryTest {

    private final PhoneNumberRequiredActionFactory factory = new PhoneNumberRequiredActionFactory();

    @Test
    void usesTheRequiredActionProviderId() {
        assertThat(factory.getId()).isEqualTo(PhoneNumberRequiredAction.PROVIDER_ID);
    }

    @Test
    void hasHumanReadableDisplayText() {
        assertThat(factory.getDisplayText()).isNotBlank();
    }

    @Test
    void createReturnsThePhoneNumberRequiredAction() {
        assertThat(factory.create(null)).isNotNull().isInstanceOf(PhoneNumberRequiredAction.class);
    }
}
