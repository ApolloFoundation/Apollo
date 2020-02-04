/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;

import java.util.Optional;

public interface RestParameter<T> {

    default boolean isEmpty(){
        return null == getRawData();
    }

    String getRawData();

    T get();

    T parse() throws RestParameterException;

    default Optional<T> optional(){
        return Optional.of(get());
    }

}
