/*
 * Copyright (c)  2021-2022 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.api.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public interface Converter<S, T> extends Function<S, T> {

    default T convert(S model) {
        T dto = null;
        if (model != null) {
            dto = this.apply(model);
        }
        return dto;
    }

    default List<T> convert(Collection<S> models) {
        List<T> dtoList;
        if (models != null && !models.isEmpty()) {
            dtoList = models.stream().map(this).collect(toList());
        } else {
            dtoList = new ArrayList<>();
        }
        return dtoList;
    }

}
