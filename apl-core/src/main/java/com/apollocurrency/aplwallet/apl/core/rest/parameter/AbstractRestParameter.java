/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;
import lombok.ToString;

@ToString
public abstract class AbstractRestParameter<T> implements RestParameter<T> {

    protected String rawData;

    protected T value = null;

    public AbstractRestParameter(String rawData) {
        this.rawData = rawData;
    }

    @Override
    public String getRawData() {
        return rawData;
    }

    @Override
    public T getIfPresent() {
        if (value == null) {
            try {
                value = parse();
            } catch (Exception ignored) {
            }
        }
        return value;
    }

    @Override
    public T get() throws RestParameterException {
        if (value == null) {
            value = parse();
        }
        return value;
    }

}
