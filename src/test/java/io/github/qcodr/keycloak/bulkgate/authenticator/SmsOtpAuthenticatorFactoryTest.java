/* Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0. */
package io.github.qcodr.keycloak.bulkgate.authenticator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;

class SmsOtpAuthenticatorFactoryTest {

    private final SmsOtpAuthenticatorFactory factory = new SmsOtpAuthenticatorFactory();

    @Test
    void usesTheStableProviderId() {
        assertThat(factory.getId()).isEqualTo("bulkgate-sms-otp-authenticator");
        assertThat(factory.getId()).isEqualTo(SmsOtpAuthenticatorFactory.PROVIDER_ID);
    }

    @Test
    void createReturnsAUsableAuthenticator() {
        assertThat(factory.create(null)).isNotNull().isInstanceOf(SmsOtpAuthenticator.class);
    }

    @Test
    void offersRequiredAlternativeAndDisabled() {
        assertThat(factory.getRequirementChoices())
                .containsExactly(Requirement.REQUIRED, Requirement.ALTERNATIVE, Requirement.DISABLED);
    }

    @Test
    void requirementChoicesAreDefensivelyCopied() {
        Requirement[] first = factory.getRequirementChoices();
        first[0] = Requirement.DISABLED; // mutate the returned array

        assertThat(factory.getRequirementChoices()[0])
                .as("internal choices must not be affected by a caller mutating the result")
                .isEqualTo(Requirement.REQUIRED);
    }

    @Test
    void isConfigurableAndAllowsUserSetup() {
        assertThat(factory.isConfigurable()).isTrue();
        assertThat(factory.isUserSetupAllowed()).isTrue();
    }

    @Test
    void exposesConfigPropertiesAndMetadata() {
        assertThat(factory.getConfigProperties()).isNotEmpty();
        assertThat(factory.getReferenceCategory()).isEqualTo("otp");
        assertThat(factory.getDisplayType()).isNotBlank();
        assertThat(factory.getHelpText()).isNotBlank();
    }
}
