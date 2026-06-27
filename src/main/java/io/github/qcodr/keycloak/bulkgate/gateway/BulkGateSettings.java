/*
 * Copyright 2026 qcodr and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Derived in part from netzbegruenung/keycloak-mfa-plugins (Apache-2.0).
 */
package io.github.qcodr.keycloak.bulkgate.gateway;

/**
 * Immutable connection settings for the BulkGate Advanced Transactional SMS API.
 *
 * <p>This is a pure value object: it carries the data needed to address BulkGate
 * and nothing about how the request is performed. Holding it separately from the
 * authenticator config keeps the gateway independent of Keycloak.</p>
 *
 * @see <a href="https://help.bulkgate.com/docs/en/http-advanced-transactional.html">BulkGate Advanced Transactional API</a>
 */
public record BulkGateSettings(
        String apiUrl,
        String applicationId,
        String applicationToken,
        String senderId,
        String senderIdValue,
        boolean unicode,
        String country) {

    public static final String DEFAULT_API_URL =
            "https://portal.bulkgate.com/api/1.0/advanced/transactional";

    /** BulkGate's neutral default sender id type when none is configured. */
    public static final String DEFAULT_SENDER_ID = "gSystem";

    public BulkGateSettings {
        apiUrl = blankToDefault(apiUrl, DEFAULT_API_URL);
        senderId = blankToDefault(senderId, DEFAULT_SENDER_ID);
        senderIdValue = senderIdValue == null ? "" : senderIdValue.trim();
        country = country == null ? "" : country.trim();
        applicationId = applicationId == null ? "" : applicationId.trim();
        applicationToken = applicationToken == null ? "" : applicationToken.trim();
    }

    /**
     * Guards live sending: BulkGate rejects requests without credentials, so we
     * fail fast with a clear message instead of leaking a vague API error.
     *
     * @throws IllegalStateException when the application id or token is missing
     */
    public void requireCredentials() {
        if (applicationId.isEmpty() || applicationToken.isEmpty()) {
            throw new IllegalStateException(
                    "BulkGate application id and token must be configured for live SMS sending");
        }
    }

    public boolean hasSenderIdValue() {
        return !senderIdValue.isEmpty();
    }

    public boolean hasCountry() {
        return !country.isEmpty();
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    @Override
    public String toString() {
        // Never log the token.
        return "BulkGateSettings[apiUrl=" + apiUrl
                + ", applicationId=" + applicationId
                + ", applicationToken=***"
                + ", senderId=" + senderId
                + ", senderIdValue=" + senderIdValue
                + ", unicode=" + unicode
                + ", country=" + country + "]";
    }
}
