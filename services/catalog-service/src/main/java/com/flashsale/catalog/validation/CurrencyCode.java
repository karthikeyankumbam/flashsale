package com.flashsale.catalog.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CurrencyCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrencyCode {
    String message() default "must be a supported three-letter currency code";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
