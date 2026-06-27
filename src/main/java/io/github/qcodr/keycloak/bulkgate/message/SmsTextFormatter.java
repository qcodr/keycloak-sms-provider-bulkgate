/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.message;

/**
 * Renders the SMS body from a template by substituting placeholders. Kept
 * separate from sending and from code generation so the wording can change (and
 * be tested) in isolation.
 *
 * <p>Supported placeholders: {@code %code%} and {@code %ttl%} (validity in
 * whole minutes).</p>
 */
public class SmsTextFormatter {

    public static final String CODE_PLACEHOLDER = "%code%";
    public static final String TTL_PLACEHOLDER = "%ttl%";

    public String format(String template, String code, long ttlMinutes) {
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("SMS text template must not be blank");
        }
        return template
                .replace(CODE_PLACEHOLDER, code)
                .replace(TTL_PLACEHOLDER, Long.toString(ttlMinutes));
    }
}
