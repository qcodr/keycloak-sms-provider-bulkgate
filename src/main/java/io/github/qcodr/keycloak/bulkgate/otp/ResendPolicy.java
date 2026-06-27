/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

import java.time.Duration;
import java.time.Instant;

/**
 * Decides whether the user may request another SMS. Enforces both a hard cap on
 * total resends and a cooldown between sends, to limit SMS cost and abuse. Pure,
 * so the throttling rules are unit-testable without a running server.
 */
public final class ResendPolicy {

    private ResendPolicy() {
    }

    public enum Decision {
        /** A new code may be sent now. */
        ALLOWED,
        /** Too soon since the last send; the cooldown has not elapsed. */
        COOLDOWN,
        /** The maximum number of resends has been reached. */
        LIMIT_REACHED
    }

    public static Decision evaluate(OtpChallenge challenge, int maxResends, Duration cooldown, Instant now) {
        if (challenge.resends() >= maxResends) {
            return Decision.LIMIT_REACHED;
        }
        Instant nextAllowed = challenge.lastSentAt().plus(cooldown);
        if (now.isBefore(nextAllowed)) {
            return Decision.COOLDOWN;
        }
        return Decision.ALLOWED;
    }
}
