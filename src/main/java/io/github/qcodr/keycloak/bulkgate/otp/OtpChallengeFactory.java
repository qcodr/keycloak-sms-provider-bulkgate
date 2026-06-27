/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.otp;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Assembles a fresh OTP: draws a code, salts and hashes it, and stamps the
 * expiry and send time. Collaborators are injected, so the whole issuance path
 * is deterministic under test by pinning the generator, salt source, and clock.
 */
public class OtpChallengeFactory {

    private final OtpCodeGenerator codeGenerator;
    private final OtpHasher hasher;
    private final SaltSource saltSource;
    private final Clock clock;

    public OtpChallengeFactory(OtpCodeGenerator codeGenerator, OtpHasher hasher, SaltSource saltSource, Clock clock) {
        this.codeGenerator = codeGenerator;
        this.hasher = hasher;
        this.saltSource = saltSource;
        this.clock = clock;
    }

    /**
     * Issues a new code for {@code recipient}, valid for {@code ttl}.
     *
     * @param priorResends  how many resends have already happened in this login
     *                      attempt (0 for the first send) — carried forward so the
     *                      resend limit survives re-issuance
     * @param priorAttempts failed verification attempts already spent in this login
     *                      attempt (0 for the first send) — carried forward so a
     *                      resend cannot reset the guessing budget. The attempt
     *                      budget is therefore per login session, not per code.
     */
    public IssuedOtp issue(int codeLength, Duration ttl, String recipient, int priorResends, int priorAttempts) {
        OtpCode code = codeGenerator.generate(codeLength);
        String salt = saltSource.newSalt();
        String hashed = hasher.hash(code.value(), salt);
        Instant now = clock.instant();
        OtpChallenge challenge =
                new OtpChallenge(hashed, salt, now.plus(ttl), priorAttempts, priorResends, recipient, now);
        return new IssuedOtp(challenge, code);
    }
}
