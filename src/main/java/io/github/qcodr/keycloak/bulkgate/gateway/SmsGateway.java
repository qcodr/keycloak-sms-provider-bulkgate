/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.gateway;

/**
 * Abstraction over an SMS delivery channel.
 *
 * <p>Depending on this interface (not on BulkGate directly) is what lets the
 * authenticator stay closed against provider changes and lets tests substitute a
 * simulation or mock — the Dependency Inversion Principle in practice.</p>
 */
public interface SmsGateway {

    /**
     * Sends the given message.
     *
     * @param message the recipient and body
     * @return the provider's API-level verdict
     * @throws SmsGatewayException when the provider could not be reached at all
     */
    SmsSendResult send(SmsMessage message) throws SmsGatewayException;
}
