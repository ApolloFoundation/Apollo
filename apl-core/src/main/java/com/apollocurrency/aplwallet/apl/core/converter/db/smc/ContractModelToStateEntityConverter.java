package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractStateEntity;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.smc.contract.SmartContract;

import javax.inject.Singleton;
import java.math.BigInteger;

@Singleton
public class ContractModelToStateEntityConverter implements Converter<SmartContract, SmcContractStateEntity> {
    @Override
    public SmcContractStateEntity apply(SmartContract model) {
        return SmcContractStateEntity.builder()
            .address(new BigInteger(model.getAddress().get()).longValueExact())
            .serializedObject(model.getSerializedObject())
            .status(model.getState().name())
            .build();
    }
}
