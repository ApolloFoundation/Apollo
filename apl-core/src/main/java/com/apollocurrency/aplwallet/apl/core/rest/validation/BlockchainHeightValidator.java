/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.validation;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import lombok.Setter;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class BlockchainHeightValidator implements ConstraintValidator<ValidBlockchainHeight, Integer> {

    @Inject @Setter
    private Blockchain blockchain;

    @Override
    public void initialize(ValidBlockchainHeight constraintAnnotation) {
    }

    @Override
    public boolean isValid(Integer integer, ConstraintValidatorContext constraintValidatorContext) {
        boolean result = null == integer || 0 < integer && integer <= blockchain.getHeight();
        if (!result) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(String.format(constraintValidatorContext.getDefaultConstraintMessageTemplate(), blockchain.getHeight())).addConstraintViolation();
        }
        return result;
    }
}
