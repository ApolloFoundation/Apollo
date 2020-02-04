/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;

public class UnsignedLongParameter extends AbstractRestParameter<Long> {

    public UnsignedLongParameter(String rawData) {
        super(rawData);
    }

    @Override
    public Long parse() throws RestParameterException {
        Long value;
        try {
            value = Long.parseUnsignedLong(getRawData());
        }catch (NumberFormatException e){
            throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, getRawData());
        }
        return value;
    }
}
