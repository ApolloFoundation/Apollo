/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

public class LongParameter extends AbstractRestParameter<Long> {

    public LongParameter(String rawData) {
        super(rawData);
    }

    @Override
    public Long parse() throws RestParameterException {
        Long value;
        try {
            value = Convert.parseLong(getRawData());
        } catch (NumberFormatException e) {
            throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, getRawData());
        }
        return value;
    }
}
