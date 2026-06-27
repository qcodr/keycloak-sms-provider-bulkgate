/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.config;

/**
 * Raised when the per-realm authenticator configuration contains a value that
 * cannot be honoured (non-numeric, out of range). Surfaced to the admin rather
 * than silently coerced, so misconfiguration is visible early.
 */
public class InvalidConfigurationException extends RuntimeException {

    public InvalidConfigurationException(String message) {
        super(message);
    }
}
