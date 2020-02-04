/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.parameter;

import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;

import javax.servlet.http.HttpServletRequest;

public class UnsignedIntegerParameter extends AbstractRestParameter<Integer> {

    public UnsignedIntegerParameter(String rawData) {
        super(rawData);
    }

    @Override
    public Integer get() {
        return value;
    }

    @Override
    public Integer parse() throws RestParameterException {
        Integer value;
        try {
            value = Integer.parseUnsignedInt(getRawData());
        }catch (NumberFormatException e){
            throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, getRawData());
        }
        return value;
    }
    public static int getLastIndex(HttpServletRequest req) {
        int lastIndex=0;/* = DEFAULT_LAST_INDEX;
        try {
            lastIndex = Integer.parseInt(req.getParameter("lastIndex"));
            if (lastIndex < 0) {
                lastIndex = DEFAULT_LAST_INDEX;
            }
        } catch (NumberFormatException ignored) {}
        if (!apw.checkPassword(req)) {
            int firstIndex = Math.min(getFirstIndex(req), Integer.MAX_VALUE - API.maxRecords + 1);
            lastIndex = Math.min(lastIndex, firstIndex + API.maxRecords - 1);
        }*/
        return lastIndex;
    }
}
