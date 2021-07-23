package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.smc.contract.SmartContract;

import javax.inject.Singleton;
import java.math.BigInteger;

@Singleton
public class ContractModelToEntityConverter implements Converter<SmartContract, SmcContractEntity> {
    @Override
    public SmcContractEntity apply(SmartContract model) {
        return SmcContractEntity.builder()
            .address(new BigInteger(model.getAddress().get()).longValueExact())
            .owner(new BigInteger(model.getOwner().get()).longValueExact())
            .transactionId(new BigInteger(model.getTxId().get()).longValueExact())
            .contractName(model.getName())
            .data(model.getSourceCode())
            .args(model.getArgs())
            .languageName(model.getLanguageName())
            .languageVersion(model.getLanguageVersion().toString())
            .status(model.getStatus().name())
            .build();
    }
}
