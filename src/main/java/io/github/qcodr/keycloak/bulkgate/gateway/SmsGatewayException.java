/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.gateway;

/**
 * Thrown when an SMS could not be delivered to the provider at all — network
 * error, timeout, or an unparseable response. Distinct from a provider-level
 * rejection, which is reported as a {@link SmsSendResult}.
 */
public class SmsGatewayException extends Exception {

    public SmsGatewayException(String message) {
        super(message);
    }

    public SmsGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
