/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.validation;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Objects;

@NoArgsConstructor
public class BlockchainHeightValidator implements ConstraintValidator<ValidBlockchainHeight, Integer> {

    @Inject
    @Setter
    private Blockchain blockchain;

    public BlockchainHeightValidator(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public void initialize(ValidBlockchainHeight constraintAnnotation) {
        Objects.requireNonNull(blockchain, "Blockchain is not injected.");
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext constraintValidatorContext) {
        boolean result = null == value || (value >= -1 && value <= blockchain.getHeight()); // -1 is special value
        if (!result) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(
                String.format(constraintValidatorContext.getDefaultConstraintMessageTemplate(), blockchain.getHeight()))
                .addConstraintViolation();
        }
        return result;
    }
}
