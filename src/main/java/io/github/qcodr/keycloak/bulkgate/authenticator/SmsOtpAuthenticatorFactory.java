/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.authenticator;

import io.github.qcodr.keycloak.bulkgate.config.ConfigProperties;
import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Registers the {@link SmsOtpAuthenticator} with Keycloak and declares its
 * per-realm configuration fields. The authenticator is stateless, so a single
 * shared instance is reused across requests.
 */
public class SmsOtpAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "bulkgate-sms-otp-authenticator";

    private static final SmsOtpAuthenticator INSTANCE = new SmsOtpAuthenticator();

    private static final Requirement[] REQUIREMENT_CHOICES = {
        Requirement.REQUIRED, Requirement.ALTERNATIVE, Requirement.DISABLED
    };

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return INSTANCE;
    }

    @Override
    public String getDisplayType() {
        return "BulkGate SMS OTP";
    }

    @Override
    public String getReferenceCategory() {
        return "otp";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES.clone();
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "Sends a one-time code by SMS through BulkGate and verifies it. "
                + "Usable as a second factor or, after a username form, as a first factor.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ConfigProperties.list();
    }

    @Override
    public void init(Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}
}
