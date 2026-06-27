/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class SimulationSmsGatewayTest {

    private final SimulationSmsGateway gateway = new SimulationSmsGateway();

    @Test
    void acceptsMessageAndReturnsSimulatedProviderMessageId() throws Exception {
        // Arrange
        SmsMessage message = new SmsMessage("+36201234567", "hello");

        // Act
        SmsSendResult result = gateway.send(message);

        // Assert
        assertThat(result.accepted()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("simulated");
        assertThat(result.errorCode()).isNull();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void sendsWithoutNetworkIoOrExceptionForNormalMessage() {
        // Arrange
        SmsMessage message = new SmsMessage("+36201234567", "hello");

        // Act + Assert: a no-network gateway must complete locally and never throw.
        assertThatCode(() -> gateway.send(message)).doesNotThrowAnyException();
    }
}
