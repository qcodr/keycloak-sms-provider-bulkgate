/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

/**
 * The possible outcomes of verifying a submitted code against a challenge.
 * Modelled as a closed set so the authenticator handles every case explicitly.
 */
public enum OtpVerificationResult {

    /** Code matched and was still within its time and attempt budget. */
    VALID,

    /** Code did not match; the attempt counter should advance. */
    INVALID,

    /** The code's time-to-live had elapsed. */
    EXPIRED,

    /** The allowed number of verification attempts was already used up. */
    TOO_MANY_ATTEMPTS,

    /** No challenge was present in the session (expired session, tampering, double submit). */
    NO_CHALLENGE
}
