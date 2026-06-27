/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.authenticator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.qcodr.keycloak.bulkgate.otp.OtpChallenge;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.ArgumentCaptor;

class OtpChallengeStoreTest {

    // Instants truncated to millis: store() serializes epoch millis, so anything
    // finer than a millisecond would be lost on the round-trip and is not a fair
    // equality target.
    private static final Instant EXPIRES_AT = Instant.ofEpochMilli(1_900_000_000_000L);
    private static final Instant LAST_SENT_AT = Instant.ofEpochMilli(1_800_000_000_123L);
    private static final String HASHED_CODE = "a1b2c3d4e5f6-hashed-otp-digest";
    private static final String SALT = "per-challenge-salt-xyz";
    private static final String RECIPIENT = "+36201234567";

    private final ObjectMapper mapper = new ObjectMapper();
    private final OtpChallengeStore store = new OtpChallengeStore(mapper);
    private final AuthenticationSessionModel session = mock(AuthenticationSessionModel.class);

    private OtpChallenge sampleChallenge() {
        return new OtpChallenge(HASHED_CODE, SALT, EXPIRES_AT, 2, 1, RECIPIENT, LAST_SENT_AT);
    }

    @Test
    void storeThenLoadReturnsAnEqualChallengeAcrossAllSevenFields() {
        OtpChallenge original = sampleChallenge();

        store.store(session, original);

        // Capture exactly what was persisted, then feed it back as the note value.
        ArgumentCaptor<String> noteCaptor = ArgumentCaptor.forClass(String.class);
        verify(session).setAuthNote(eq(OtpChallengeStore.NOTE), noteCaptor.capture());
        when(session.getAuthNote(OtpChallengeStore.NOTE)).thenReturn(noteCaptor.getValue());

        OtpChallenge loaded = store.load(session);

        // Record equality covers every component, but assert each one explicitly so a
        // single drifting field names itself in the failure.
        assertThat(loaded).isEqualTo(original);
        assertThat(loaded.hashedCode()).isEqualTo(HASHED_CODE);
        assertThat(loaded.salt()).isEqualTo(SALT);
        assertThat(loaded.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(loaded.attempts()).isEqualTo(2);
        assertThat(loaded.resends()).isEqualTo(1);
        assertThat(loaded.recipient()).isEqualTo(RECIPIENT);
        assertThat(loaded.lastSentAt()).isEqualTo(LAST_SENT_AT);
    }

    @Test
    void loadReturnsNullWhenNoteIsAbsent() {
        when(session.getAuthNote(OtpChallengeStore.NOTE)).thenReturn(null);

        assertThat(store.load(session)).isNull();
    }

    @Test
    void loadReturnsNullWhenNoteIsBlank() {
        when(session.getAuthNote(OtpChallengeStore.NOTE)).thenReturn("");

        assertThat(store.load(session)).isNull();
    }

    @Test
    void loadReturnsNullWhenNoteIsCorruptJson() {
        when(session.getAuthNote(OtpChallengeStore.NOTE)).thenReturn("{not valid");

        assertThat(store.load(session)).isNull();
    }

    @Test
    void clearRemovesTheNoteUnderTheStoreKey() {
        store.clear(session);

        verify(session).removeAuthNote(OtpChallengeStore.NOTE);
    }

    @Test
    void storeWritesNonBlankNoteContainingOnlyTheHashedCode() {
        store.store(session, sampleChallenge());

        ArgumentCaptor<String> noteCaptor = ArgumentCaptor.forClass(String.class);
        verify(session).setAuthNote(eq(OtpChallengeStore.NOTE), noteCaptor.capture());
        String note = noteCaptor.getValue();

        // Only the hash of the code is persisted — never a plaintext OTP.
        assertThat(note).isNotBlank();
        assertThat(note).contains(HASHED_CODE);
    }
}
