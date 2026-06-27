/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.authenticator;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.qcodr.keycloak.bulkgate.config.ConfigKeys;
import io.github.qcodr.keycloak.bulkgate.config.SmsAuthenticatorConfig;
import io.github.qcodr.keycloak.bulkgate.gateway.BulkGateSmsGateway;
import io.github.qcodr.keycloak.bulkgate.gateway.SimulationSmsGateway;
import io.github.qcodr.keycloak.bulkgate.gateway.SmsGateway;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SmsGatewayResolverTest {

    private final SmsGatewayResolver resolver =
            new SmsGatewayResolver(HttpClient.newHttpClient(), new ObjectMapper(), Duration.ofSeconds(5));

    @Test
    void resolvesSimulationGatewayWhenSimulationModeEnabled() {
        // Arrange
        SmsAuthenticatorConfig config = SmsAuthenticatorConfig.from(Map.of(ConfigKeys.SIMULATION_MODE, "true"));

        // Act
        SmsGateway gateway = resolver.resolve(config);

        // Assert
        assertThat(gateway).isInstanceOf(SimulationSmsGateway.class);
    }

    @Test
    void resolvesBulkGateGatewayWhenSimulationModeDisabled() {
        // Arrange
        SmsAuthenticatorConfig config = SmsAuthenticatorConfig.from(Map.of(
                ConfigKeys.SIMULATION_MODE, "false",
                ConfigKeys.BULKGATE_APPLICATION_ID, "app-1",
                ConfigKeys.BULKGATE_APPLICATION_TOKEN, "token-1"));

        // Act
        SmsGateway gateway = resolver.resolve(config);

        // Assert
        assertThat(gateway).isInstanceOf(BulkGateSmsGateway.class);
    }
}
