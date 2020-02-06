package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.exchange.DexConfig;
import lombok.Setter;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
