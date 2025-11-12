package com.flippa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConditionalWebsiteUrlValidator.class)
@Documented
public @interface ConditionalWebsiteUrl {
    String message() default "Website URL is required for Website and Domain listings";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

