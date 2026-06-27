/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.gateway;

import org.jboss.logging.Logger;

/**
 * A no-network gateway that writes the message to the server log instead of
 * sending it. Used when the realm enables simulation mode, so the login flow can
 * be exercised end to end without spending SMS credit or configuring BulkGate.
 */
public class SimulationSmsGateway implements SmsGateway {

    private static final Logger LOG = Logger.getLogger(SimulationSmsGateway.class);

    @Override
    public SmsSendResult send(SmsMessage message) {
        // The code is logged on purpose so the flow can be completed without a real
        // SMS. This is a development aid only: simulation mode must never be enabled
        // in production, where it would write every OTP to the server log.
        LOG.warnf("[SIMULATION — DO NOT USE IN PRODUCTION] SMS to %s: %s", message.recipient(), message.text());
        return SmsSendResult.accepted("simulated");
    }
}
