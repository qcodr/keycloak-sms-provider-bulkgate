/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

/**
 * The two faces of a newly issued OTP: the {@link OtpChallenge} (hashed, for
 * storage) and the plaintext {@link OtpCode} (transient, for the SMS). Returning
 * both together keeps the plaintext from ever needing to be re-derived.
 */
public record IssuedOtp(OtpChallenge challenge, OtpCode plaintext) {}
