/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.dex.core.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
    ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AtomicSwapTimeValidator.class)
@Documented
public @interface ValidAtomicSwapTime {
    String message() default "Not a valid atomic swap duration, required in range %d;%d ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
