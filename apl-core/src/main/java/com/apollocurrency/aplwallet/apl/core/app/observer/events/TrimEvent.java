/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app.observer.events;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
/**
 *
 * @author alukin@gmail.com
 */
@Qualifier

@Target({METHOD, FIELD, PARAMETER, TYPE})

@Retention(RUNTIME)

public @interface TrimEvent {
    
}
