/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.validation;

import com.apollocurrency.aplwallet.apl.core.rest.provider.PlatformSpecs;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PlatformSpecsValidator implements ConstraintValidator<ValidPlatformSpecs, PlatformSpecs> {

    @Override
    public void initialize(ValidPlatformSpecs constraintAnnotation) {
    }

    @Override
    public boolean isValid(PlatformSpecs specs, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = specs != null && !specs.getSpecList().isEmpty();
        if (!isValid) {
            constraintValidatorContext.buildConstraintViolationWithTemplate(constraintValidatorContext.getDefaultConstraintMessageTemplate());
        }
        return isValid;
    }
}
