/* Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0. */
package io.github.qcodr.keycloak.bulkgate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.keycloak.provider.ProviderConfigProperty;

class ConfigPropertiesTest {

    @Test
    void exposesAnAdminFieldForEveryConfigurationKey() {
        Set<String> names = ConfigProperties.list().stream()
                .map(ProviderConfigProperty::getName)
                .collect(Collectors.toSet());

        assertThat(names)
                .contains(
                        ConfigKeys.CODE_LENGTH,
                        ConfigKeys.CODE_TTL_SECONDS,
                        ConfigKeys.MAX_VERIFY_ATTEMPTS,
                        ConfigKeys.RESEND_COOLDOWN_SECONDS,
                        ConfigKeys.MAX_RESENDS,
                        ConfigKeys.SMS_TEXT_TEMPLATE,
                        ConfigKeys.PHONE_NUMBER_ATTRIBUTE,
                        ConfigKeys.PHONE_NUMBER_VERIFIED_ATTRIBUTE,
                        ConfigKeys.MARK_PHONE_VERIFIED,
                        ConfigKeys.DEFAULT_COUNTRY_CODE,
                        ConfigKeys.SIMULATION_MODE,
                        ConfigKeys.BULKGATE_API_URL,
                        ConfigKeys.BULKGATE_APPLICATION_ID,
                        ConfigKeys.BULKGATE_APPLICATION_TOKEN,
                        ConfigKeys.BULKGATE_SENDER_ID,
                        ConfigKeys.BULKGATE_SENDER_ID_VALUE,
                        ConfigKeys.BULKGATE_UNICODE,
                        ConfigKeys.BULKGATE_COUNTRY);
    }

    @Test
    void marksOnlyTheApplicationTokenAsSecret() {
        List<ProviderConfigProperty> props = ConfigProperties.list();

        Set<String> secret = props.stream()
                .filter(ProviderConfigProperty::isSecret)
                .map(ProviderConfigProperty::getName)
                .collect(Collectors.toSet());

        assertThat(secret).containsExactly(ConfigKeys.BULKGATE_APPLICATION_TOKEN);
    }

    @Test
    void carriesWorkingDefaultsSoOnlyCredentialsMustBeEntered() {
        var byName =
                ConfigProperties.list().stream().collect(Collectors.toMap(ProviderConfigProperty::getName, p -> p));

        // Sensible defaults are pre-filled...
        assertThat(byName.get(ConfigKeys.CODE_LENGTH).getDefaultValue())
                .isEqualTo(Integer.toString(ConfigKeys.DEFAULT_CODE_LENGTH));
        assertThat(byName.get(ConfigKeys.BULKGATE_SENDER_ID).getDefaultValue()).isEqualTo("gSystem");
        assertThat(byName.get(ConfigKeys.PHONE_NUMBER_ATTRIBUTE).getDefaultValue())
                .isEqualTo("phoneNumber");
        // ...while the credentials are intentionally blank.
        assertThat(byName.get(ConfigKeys.BULKGATE_APPLICATION_ID).getDefaultValue())
                .isEqualTo("");
        assertThat(byName.get(ConfigKeys.BULKGATE_APPLICATION_TOKEN).getDefaultValue())
                .isEqualTo("");
    }
}
