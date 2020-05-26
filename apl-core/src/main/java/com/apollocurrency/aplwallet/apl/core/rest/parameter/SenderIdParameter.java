/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.utils.RestParametersParser;

public class SenderIdParameter extends AbstractRestParameter<Long> {

    public SenderIdParameter(String sender) {
        super(sender);
    }

    @Override
    public Long parse() throws RestParameterException {
        return RestParametersParser.parseAccountId(getRawData(), "sender");
    }
}
