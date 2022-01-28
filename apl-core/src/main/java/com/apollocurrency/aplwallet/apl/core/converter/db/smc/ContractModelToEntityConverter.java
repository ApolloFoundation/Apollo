/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.smc.contract.SmartContract;

import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class ContractModelToEntityConverter implements Converter<SmartContract, SmcContractEntity> {
    @Override
    public SmcContractEntity apply(SmartContract model) {
        return SmcContractEntity.builder()
                .address(new AplAddress(model.address()).getLongId())
            .owner(new AplAddress(model.getOwner()).getLongId())
            .transactionId(new AplAddress(model.getTxId()).getLongId())
            .fuelPrice(model.getFuel().price().longValueExact())
            .fuelLimit(model.getFuel().limit().longValueExact())
            .fuelCharged(model.getFuel().charged().longValueExact())
            .contractName(model.getName())
            .baseContract(model.getBaseContract())
            .data(model.getSourceCode())
            .args(model.getArgs())
            .languageName(model.getLanguageName())
            .languageVersion(model.getLanguageVersion().toString())
            .status(model.getStatus().name())
            .build();
    }
}
