/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.dex.core.validation;

import com.apollocurrency.aplwallet.apl.dex.config.DexConfig;
import lombok.Setter;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AtomicSwapTimeValidator implements ConstraintValidator<ValidAtomicSwapTime, Integer> {
    @Inject
    @Setter
    DexConfig dexConfig;

    @Override
    public void initialize(ValidAtomicSwapTime constraintAnnotation) {
    }

    @Override
    public boolean isValid(Integer integer, ConstraintValidatorContext constraintValidatorContext) {
        boolean result = integer != null && dexConfig.getMaxAtomicSwapDuration() >= integer && dexConfig.getMinAtomicSwapDuration() <= integer;
        if (!result) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(String.format(constraintValidatorContext.getDefaultConstraintMessageTemplate(), dexConfig.getMinAtomicSwapDuration(), dexConfig.getMaxAtomicSwapDuration())).addConstraintViolation();
        }
        return result;
    }
}
