/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.parameter;


import com.apollocurrency.aplwallet.apl.core.rest.utils.RestParametersParser;
import com.apollocurrency.aplwallet.apl.util.api.parameter.AbstractRestParameter;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;

//TODO move to util module
public class AccountIdParameter extends AbstractRestParameter<Long> {

    public AccountIdParameter(String account) {
        super(account);
    }

    @Override
    public Long parse() throws RestParameterException {
        return RestParametersParser.parseAccountId(getRawData());
    }
}
