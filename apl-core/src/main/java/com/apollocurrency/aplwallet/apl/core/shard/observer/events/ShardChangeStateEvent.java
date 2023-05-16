/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer.events;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;

import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Qualifier

@Target({METHOD, FIELD, PARAMETER, TYPE})

@Retention(RUNTIME)

public @interface ShardChangeStateEvent {

    MigrateState value();

}
