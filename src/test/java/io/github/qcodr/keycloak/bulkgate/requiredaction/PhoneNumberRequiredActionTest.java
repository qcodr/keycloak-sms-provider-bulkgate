/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.requiredaction;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.qcodr.keycloak.bulkgate.config.ConfigKeys;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.UserModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhoneNumberRequiredActionTest {

    @Mock
    private RequiredActionContext context;

    @Mock
    private LoginFormsProvider form;

    @Mock
    private UserModel user;

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private Response response;

    private final PhoneNumberRequiredAction action = new PhoneNumberRequiredAction();

    private void stubFormData(String mobileNumber) {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.putSingle(PhoneNumberRequiredAction.FORM_FIELD, mobileNumber);
        when(context.getHttpRequest()).thenReturn(httpRequest);
        when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
    }

    private void stubFluentForm() {
        when(context.form()).thenReturn(form);
        when(form.setAttribute(anyString(), any())).thenReturn(form);
        when(form.createForm(anyString())).thenReturn(response);
    }

    @Test
    void processAction_validNumber_storesNormalizedPhoneAndSucceeds() {
        // Arrange: "06201234567" normalizes (HU default +36) to E.164 +36201234567.
        stubFormData("06201234567");
        when(context.getUser()).thenReturn(user);

        // Act
        action.processAction(context);

        // Assert
        verify(user).setSingleAttribute(ConfigKeys.DEFAULT_PHONE_NUMBER_ATTRIBUTE, "+36201234567");
        verify(user).setSingleAttribute(ConfigKeys.DEFAULT_PHONE_NUMBER_VERIFIED_ATTRIBUTE, "false");
        verify(context).success();
        verify(context, never()).challenge(any(Response.class));
    }

    @Test
    void processAction_alreadyInternationalNumber_storesItVerbatimAndSucceeds() {
        // Arrange
        stubFormData("+36201234567");
        when(context.getUser()).thenReturn(user);

        // Act
        action.processAction(context);

        // Assert
        verify(user).setSingleAttribute(ConfigKeys.DEFAULT_PHONE_NUMBER_ATTRIBUTE, "+36201234567");
        verify(user).setSingleAttribute(ConfigKeys.DEFAULT_PHONE_NUMBER_VERIFIED_ATTRIBUTE, "false");
        verify(context).success();
        verify(context, never()).challenge(any(Response.class));
    }

    @Test
    void processAction_invalidNumber_reChallengesWithErrorAndStoresNothing() {
        // Arrange
        stubFormData("abc");
        stubFluentForm();
        when(form.setError(anyString())).thenReturn(form);

        // Act
        action.processAction(context);

        // Assert: failure path -> no success, a challenge is shown with the invalid-phone error.
        verify(context, never()).success();
        verify(context).challenge(response);
        verify(form).setError(PhoneNumberRequiredAction.MSG_INVALID_PHONE);
        verify(form).createForm(PhoneNumberRequiredAction.FORM);
        // The rejected number must not be persisted to the user.
        verify(user, never()).setSingleAttribute(anyString(), anyString());
    }

    @Test
    void processAction_invalidNumber_echoesRawInputBackIntoForm() {
        // Arrange
        stubFormData("abc");
        stubFluentForm();
        when(form.setError(anyString())).thenReturn(form);

        // Act
        action.processAction(context);

        // Assert: the raw value is re-rendered so the user can correct it.
        verify(form).setAttribute(eq(PhoneNumberRequiredAction.FORM_FIELD), eq("abc"));
    }

    @Test
    void requiredActionChallenge_rendersFormPrefilledWithCurrentNumber() {
        // Arrange
        when(context.getUser()).thenReturn(user);
        when(user.getFirstAttribute(ConfigKeys.DEFAULT_PHONE_NUMBER_ATTRIBUTE)).thenReturn("+36201234567");
        stubFluentForm();

        // Act
        action.requiredActionChallenge(context);

        // Assert
        verify(form).setAttribute(eq(PhoneNumberRequiredAction.FORM_FIELD), eq("+36201234567"));
        verify(form).createForm(PhoneNumberRequiredAction.FORM);
        verify(context).challenge(response);
    }

    @Test
    void requiredActionChallenge_usesEmptyStringWhenNoNumberStored() {
        // Arrange
        when(context.getUser()).thenReturn(user);
        when(user.getFirstAttribute(ConfigKeys.DEFAULT_PHONE_NUMBER_ATTRIBUTE)).thenReturn(null);
        stubFluentForm();

        // Act
        action.requiredActionChallenge(context);

        // Assert: null current value is rendered as "" (never the literal "null").
        verify(form).setAttribute(eq(PhoneNumberRequiredAction.FORM_FIELD), eq(""));
        verify(context).challenge(response);
    }

    @Test
    void evaluateTriggers_doesNothingAndDoesNotThrow() {
        // Act + Assert
        assertThatCode(() -> action.evaluateTriggers(context)).doesNotThrowAnyException();
        verifyNoInteractions(context);
    }
}
