/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;

public class IdParameter extends UnsignedLongParameter {

    public IdParameter(String rawData) {
        super(rawData);
    }

    @Override
    public Long parse() throws RestParameterException {
        Long value = super.parse();
        if (value == 0){
            throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, getRawData());
        }
        return value;
    }
}
