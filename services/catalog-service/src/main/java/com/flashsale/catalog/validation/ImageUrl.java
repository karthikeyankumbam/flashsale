package com.flashsale.catalog.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ImageUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImageUrl {
    String message() default "must be an absolute HTTP or HTTPS URL without credentials";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
