package com.flashsale.catalog.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Currency;
import java.util.Locale;

public class CurrencyCodeValidator implements ConstraintValidator<CurrencyCode, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        try {
            Currency.getInstance(value.trim().toUpperCase(Locale.ROOT));
            return value.trim().length() == 3;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
