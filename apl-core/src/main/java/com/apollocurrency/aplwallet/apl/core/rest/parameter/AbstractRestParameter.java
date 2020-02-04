/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractRestParameter<T> implements RestParameter<T> {

    protected String rawData;

    protected T value = null;

    public AbstractRestParameter(String rawData) {
        this.rawData = StringUtils.isBlank(rawData)? null: rawData;
    }

    @Override
    public String getRawData() {
        return rawData;
    }

    @Override
    public T get() {
        if(value == null){
            try{
                value = parse();
            }catch (Exception ignored){}
        }
        return value;
    }
}
