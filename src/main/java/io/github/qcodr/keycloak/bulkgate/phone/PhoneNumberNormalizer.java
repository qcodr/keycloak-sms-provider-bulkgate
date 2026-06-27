/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.phone;

/**
 * Turns whatever a user typed or stored into a canonical {@link PhoneNumber}.
 * An interface so a heavier library (e.g. libphonenumber) can replace the
 * default heuristics without changing callers.
 */
public interface PhoneNumberNormalizer {

    /**
     * @param raw a user- or attribute-supplied number in any common notation
     * @return the canonical E.164 number
     * @throws InvalidPhoneNumberException if the input cannot be made valid
     */
    PhoneNumber normalize(String raw);
}
