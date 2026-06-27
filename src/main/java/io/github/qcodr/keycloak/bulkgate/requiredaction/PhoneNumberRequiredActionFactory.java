/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.requiredaction;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Registers the {@link PhoneNumberRequiredAction}. The action is stateless, so a
 * single shared instance is reused.
 */
public class PhoneNumberRequiredActionFactory implements RequiredActionFactory {

    private static final PhoneNumberRequiredAction INSTANCE = new PhoneNumberRequiredAction();

    @Override
    public String getId() {
        return PhoneNumberRequiredAction.PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "Configure BulkGate SMS phone number";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return INSTANCE;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}
