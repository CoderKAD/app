package com.restaurantapp.demo.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.restaurantapp.demo.exception.BadRequestException;

public class PhoneNumberUtils {

    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    /**
     * Normalize and validate phone number for specific region (Morocco "MA")
     * @param phone Raw phone number (with or without country code)
     * @param region ISO country code (e.g., "MA" for Morocco)
     * @return E.164 formatted phone number if valid
     * @throws BadRequestException if phone is invalid for the region
     */
    public static String normalize(String phone, String region) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new BadRequestException("Phone number cannot be empty");
        }

        try {
            // Parse and validate phone number
            PhoneNumber phoneProto = phoneUtil.parse(phone.trim(), region);

            // Validate for specific region (Morocco)
            if (!phoneUtil.isValidNumberForRegion(phoneProto, region)) {
                throw new BadRequestException(
                        String.format("Invalid phone number for %s. Expected format: +212 6XX XXX XXX or 06XX XXX XXX", region)
                );
            }

            // Return E.164 format (+212XXXXXXXXX)
            return phoneUtil.format(phoneProto, PhoneNumberUtil.PhoneNumberFormat.E164);

        } catch (NumberParseException e) {
            throw new BadRequestException(
                    "Invalid phone format. Moroccan numbers should be: 06XX XXX XXX or +212 6XX XXX XXX"
            );
        }
    }

    /**
     * Quick validation without normalization
     */
    public static boolean isValid(String phone, String region) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        try {
            PhoneNumber phoneProto = phoneUtil.parse(phone.trim(), region);
            return phoneUtil.isValidNumberForRegion(phoneProto, region);
        } catch (NumberParseException e) {
            return false;
        }
    }
}