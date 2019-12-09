package com.apollocurrency.aplwallet.apl.util.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author silaev-firstbridge on 11/22/2019
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DatabaseSpecificDmls.class)
public @interface DatabaseSpecificDml {
    DmlMarker[] value();
}
