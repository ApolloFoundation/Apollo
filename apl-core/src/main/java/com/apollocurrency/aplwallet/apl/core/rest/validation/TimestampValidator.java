/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.validation;

import lombok.NoArgsConstructor;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@NoArgsConstructor
public class TimestampValidator implements ConstraintValidator<ValidTimestamp, Integer> {

    @Override
    public void initialize(ValidTimestamp constraintAnnotation) {
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext constraintValidatorContext) {
        boolean result = null == value || value == -1 || 0 < value;
        if (!result) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(
                String.format(constraintValidatorContext.getDefaultConstraintMessageTemplate(), value))
                .addConstraintViolation();
        }
        return result;
    }
}
