/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RestParameterValidator.class)
public @interface Validate {
    String message() default "";
    /**
     * The name of the parameter.
     *
     * @return the parameter's name
     **/
    String name() default "";

    /**
     * Determines whether this parameter is mandatory.
     *
     * @return whether or not the parameter is required
     **/
    boolean required() default false;

    /**
     * Default value fro this parameter.
     *
     * @return the deefault value for this parameter
     **/
    String defaultValue() default "";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
