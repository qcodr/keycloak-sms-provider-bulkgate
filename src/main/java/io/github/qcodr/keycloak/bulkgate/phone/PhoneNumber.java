/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.phone;

import java.util.regex.Pattern;

/**
 * A phone number in E.164 form, e.g. {@code +420777123456}. Construction enforces
 * the format, so any {@code PhoneNumber} in the system is known-valid (making
 * invalid state unrepresentable downstream).
 */
public record PhoneNumber(String e164) {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    public PhoneNumber {
        if (e164 == null || !E164.matcher(e164).matches()) {
            throw new InvalidPhoneNumberException("Not a valid E.164 phone number: " + e164);
        }
    }

    /** The number without its leading {@code +}, as several SMS APIs expect. */
    public String withoutPlus() {
        return e164.substring(1);
    }
}
