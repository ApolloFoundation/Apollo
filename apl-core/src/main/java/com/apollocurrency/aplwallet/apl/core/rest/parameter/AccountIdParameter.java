/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import com.apollocurrency.aplwallet.apl.core.rest.utils.RestParametersParser;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;

public class AccountIdParameter extends AbstractRestParameter<Long> {

    public AccountIdParameter(String rawData) {
        super(rawData);
    }

    @Override
    public Long parse() throws RestParameterException {
        return RestParametersParser.parseAccountId(getRawData());
    }
}
