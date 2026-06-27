/* Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0. */
package io.github.qcodr.keycloak.bulkgate.authenticator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.qcodr.keycloak.bulkgate.requiredaction.PhoneNumberRequiredAction;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

class SmsOtpAuthenticatorSpiTest {

    private final SmsOtpAuthenticator authenticator = new SmsOtpAuthenticator();
    private final KeycloakSession session = mock(KeycloakSession.class);
    private final RealmModel realm = mock(RealmModel.class);
    private final UserModel user = mock(UserModel.class);

    @Test
    void requiresUserSoItCanRunAsFirstOrSecondFactor() {
        assertThat(authenticator.requiresUser()).isTrue();
    }

    @Test
    void isConfiguredForWhenTheUserHasAPhoneNumber() {
        when(user.getFirstAttribute("phoneNumber")).thenReturn("+36201234567");

        assertThat(authenticator.configuredFor(session, realm, user)).isTrue();
    }

    @Test
    void isNotConfiguredForWhenThePhoneNumberIsMissingOrBlank() {
        when(user.getFirstAttribute("phoneNumber")).thenReturn(null);
        assertThat(authenticator.configuredFor(session, realm, user)).isFalse();

        when(user.getFirstAttribute("phoneNumber")).thenReturn("  ");
        assertThat(authenticator.configuredFor(session, realm, user)).isFalse();
    }

    @Test
    void setRequiredActionsAddsThePhoneEnrollmentAction() {
        authenticator.setRequiredActions(session, realm, user);

        verify(user).addRequiredAction(PhoneNumberRequiredAction.PROVIDER_ID);
    }

    @Test
    void closeIsANoOp() {
        assertThatCode(authenticator::close).doesNotThrowAnyException();
    }
}
