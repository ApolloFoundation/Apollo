/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.utils.RestParametersParser;

public class RecipientIdParameter extends AbstractRestParameter<Long> {

    public RecipientIdParameter(String recipient) {
        super(recipient);
    }

    @Override
    public Long parse() throws RestParameterException {
        return RestParametersParser.parseAccountId(getRawData(), "recipient");
    }
}
