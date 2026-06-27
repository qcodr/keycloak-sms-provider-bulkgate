/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.authenticator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.qcodr.keycloak.bulkgate.config.SmsAuthenticatorConfig;
import io.github.qcodr.keycloak.bulkgate.gateway.BulkGateSmsGateway;
import io.github.qcodr.keycloak.bulkgate.gateway.SimulationSmsGateway;
import io.github.qcodr.keycloak.bulkgate.gateway.SmsGateway;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Chooses the {@link SmsGateway} for a given realm configuration: a real BulkGate
 * client, or a no-network simulation when simulation mode is on. Lives in the
 * authenticator layer (which already depends on both config and gateway) to keep
 * those two packages free of a circular dependency.
 */
public class SmsGatewayResolver {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final Duration requestTimeout;

    public SmsGatewayResolver(HttpClient httpClient, ObjectMapper mapper, Duration requestTimeout) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.requestTimeout = requestTimeout;
    }

    public SmsGateway resolve(SmsAuthenticatorConfig config) {
        if (config.simulationMode()) {
            return new SimulationSmsGateway();
        }
        return new BulkGateSmsGateway(httpClient, mapper, config.bulkGate(), requestTimeout);
    }
}
