/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.authenticator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.qcodr.keycloak.bulkgate.otp.OtpChallenge;
import java.io.IOException;
import java.time.Instant;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Persists the {@link OtpChallenge} in the authentication session as a single
 * JSON note. Isolating Keycloak's note API here keeps the authenticator focused
 * on flow logic and makes the stored shape explicit in one place.
 *
 * <p>Only the hashed code is written — never the plaintext — so the note is safe
 * even though it lives in the (cookie-backed) auth session.</p>
 */
public class OtpChallengeStore {

    static final String NOTE = "bulkgate.otp.challenge";

    private final ObjectMapper mapper;

    public OtpChallengeStore(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void store(AuthenticationSessionModel session, OtpChallenge challenge) {
        ObjectNode node = mapper.createObjectNode();
        node.put("hashedCode", challenge.hashedCode());
        node.put("salt", challenge.salt());
        node.put("expiresAt", challenge.expiresAt().toEpochMilli());
        node.put("attempts", challenge.attempts());
        node.put("resends", challenge.resends());
        node.put("recipient", challenge.recipient());
        node.put("lastSentAt", challenge.lastSentAt().toEpochMilli());
        try {
            session.setAuthNote(NOTE, mapper.writeValueAsString(node));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize OTP challenge", e);
        }
    }

    /**
     * @return the stored challenge, or {@code null} if none is present (fresh
     *         session, already cleared, or double submit)
     */
    public OtpChallenge load(AuthenticationSessionModel session) {
        String raw = session.getAuthNote(NOTE);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode node = mapper.readTree(raw);
            return new OtpChallenge(
                    node.path("hashedCode").asText(),
                    node.path("salt").asText(),
                    Instant.ofEpochMilli(node.path("expiresAt").asLong()),
                    node.path("attempts").asInt(),
                    node.path("resends").asInt(),
                    node.path("recipient").asText(),
                    Instant.ofEpochMilli(node.path("lastSentAt").asLong()));
        } catch (IOException | RuntimeException e) {
            // Corrupt/garbage note: treat as no challenge rather than 500.
            return null;
        }
    }

    public void clear(AuthenticationSessionModel session) {
        session.removeAuthNote(NOTE);
    }
}
