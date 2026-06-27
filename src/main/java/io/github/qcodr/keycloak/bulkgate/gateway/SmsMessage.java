/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.gateway;

import java.util.Objects;

/**
 * A single SMS to deliver: who receives it and what it says.
 *
 * <p>The recipient is expected in E.164 form (e.g. {@code +420777123456}).
 * Gateways are free to reformat it for their wire protocol.</p>
 */
public record SmsMessage(String recipient, String text) {

    public SmsMessage {
        Objects.requireNonNull(recipient, "recipient");
        Objects.requireNonNull(text, "text");
        if (recipient.isBlank()) {
            throw new IllegalArgumentException("recipient must not be blank");
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
    }
}
