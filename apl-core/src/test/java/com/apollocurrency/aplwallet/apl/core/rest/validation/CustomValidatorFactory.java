/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.validation;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import java.util.Map;

public class CustomValidatorFactory implements ConstraintValidatorFactory {
    private ConstraintValidatorFactory delegate;
    private Map<Class<?>, ConstraintValidator<?, ?>> validators;

    public CustomValidatorFactory(Map<Class<?>, ConstraintValidator<?, ?>> validators) {
        this.delegate = new ConstraintValidatorFactoryImpl();
        this.validators = validators;
    }

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
        ConstraintValidator<?, ?> validator = validators.get(key);
        return (validator != null) ? (T) validator : delegate.getInstance(key);
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
        delegate.releaseInstance(instance);
    }
}
