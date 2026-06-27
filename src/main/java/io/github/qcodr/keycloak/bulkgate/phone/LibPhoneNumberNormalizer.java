/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.phone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

/**
 * {@link PhoneNumberNormalizer} backed by Google's libphonenumber.
 *
 * <p>National trunk prefixes are country-specific (one digit {@code 0} in
 * Germany, two digits {@code 06} in Hungary, ...), so a hand-rolled stripper is
 * wrong somewhere. libphonenumber knows each region's rules and also validates
 * that the result is a real, dialable number.</p>
 *
 * <p>The configured default dialing code (e.g. {@code +36}) selects the region
 * used to interpret numbers typed without an international prefix.</p>
 */
public class LibPhoneNumberNormalizer implements PhoneNumberNormalizer {

    private final PhoneNumberUtil util = PhoneNumberUtil.getInstance();
    private final String defaultRegion;

    /**
     * @param defaultCountryCode dialing code such as {@code +36} or {@code 36}
     */
    public LibPhoneNumberNormalizer(String defaultCountryCode) {
        this.defaultRegion = util.getRegionCodeForCountryCode(parseDialingCode(defaultCountryCode));
    }

    @Override
    public PhoneNumber normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidPhoneNumberException("Phone number is empty");
        }
        try {
            Phonenumber.PhoneNumber parsed = util.parse(raw, defaultRegion);
            if (!util.isValidNumber(parsed)) {
                throw new InvalidPhoneNumberException("Not a valid phone number: " + raw);
            }
            return new PhoneNumber(util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164));
        } catch (NumberParseException e) {
            throw new InvalidPhoneNumberException("Could not parse phone number: " + raw);
        }
    }

    private static int parseDialingCode(String defaultCountryCode) {
        if (defaultCountryCode == null || defaultCountryCode.isBlank()) {
            throw new IllegalArgumentException("defaultCountryCode must not be blank");
        }
        String digits = defaultCountryCode.trim().replaceFirst("^\\+", "");
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("defaultCountryCode must be a dialing code, was " + defaultCountryCode);
        }
    }
}
