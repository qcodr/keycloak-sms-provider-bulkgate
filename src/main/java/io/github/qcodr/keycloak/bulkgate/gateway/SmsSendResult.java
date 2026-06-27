/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.gateway;

/**
 * Outcome of an SMS send attempt at the API level (the request reached the
 * provider and got a verdict). Transport failures are signalled separately via
 * {@link SmsGatewayException} so callers can tell "provider said no" from
 * "we never reached the provider".
 *
 * <p>On success {@code providerMessageId} is set and the error fields are
 * {@code null}; on rejection the opposite holds.</p>
 */
public record SmsSendResult(boolean accepted, String providerMessageId, String errorCode, String errorMessage) {

    public static SmsSendResult accepted(String providerMessageId) {
        return new SmsSendResult(true, providerMessageId, null, null);
    }

    public static SmsSendResult rejected(String errorCode, String errorMessage) {
        return new SmsSendResult(false, null, errorCode, errorMessage);
    }
}
