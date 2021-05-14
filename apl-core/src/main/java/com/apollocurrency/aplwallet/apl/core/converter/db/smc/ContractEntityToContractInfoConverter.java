package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.api.v2.model.ContractInfo;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

import javax.inject.Singleton;

@Singleton
public class ContractEntityToContractInfoConverter implements Converter<SmcContractEntity, ContractInfo> {
    @Override
    public ContractInfo apply(SmcContractEntity entity) {
        ContractInfo dto = new ContractInfo();
        dto.setAddress(new AplAddress(entity.getAddress()).getHex());
        dto.setTransaction(new AplAddress(entity.getTransactionId()).getHex());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
