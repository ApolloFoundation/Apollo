/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractStateEntity;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplSmcContract;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class SmcContractStateToEntityConverter implements Converter<AplSmcContract, SmcContractStateEntity> {
    @Override
    public SmcContractStateEntity apply(AplSmcContract model) {

        return SmcContractStateEntity.builder()
            .address(new BigInteger(model.getAddress().get()).longValueExact())
            .serializedObject(model.getSerializedObject())
            .status(model.getStatus())
            .build();
    }
}
