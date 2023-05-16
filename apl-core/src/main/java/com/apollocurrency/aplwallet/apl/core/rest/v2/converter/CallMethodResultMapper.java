/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.converter;

import com.apollocurrency.aplwallet.api.v2.model.CallMethodResult;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.smc.contract.vm.ResultValue;

import jakarta.inject.Singleton;
import java.util.stream.Collectors;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Singleton
public class CallMethodResultMapper implements Converter<ResultValue, CallMethodResult> {

    @Override
    public CallMethodResult apply(ResultValue model) {
        var dto = new CallMethodResult();
        dto.setMethod(model.getMethod());
        dto.setSignature(model.getSignature());
        dto.setOutput(model.getOutput().stream().map(Object::toString).collect(Collectors.toList()));
        dto.setErrorCode(model.getErrorCode());
        dto.setErrorDescription(model.getErrorDescription());
        return dto;
    }

}
