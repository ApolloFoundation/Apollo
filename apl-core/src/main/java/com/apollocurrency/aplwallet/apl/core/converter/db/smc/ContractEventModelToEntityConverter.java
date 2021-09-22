package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEventEntity;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractEvent;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

import javax.inject.Singleton;

@Singleton
public class ContractEventModelToEntityConverter implements Converter<AplContractEvent, SmcContractEventEntity> {
    @Override
    public SmcContractEventEntity apply(AplContractEvent model) {
        return SmcContractEventEntity.builder()
            .contract(model.getContractId())
            .id(model.getId())
            .signature(model.getSignature())
            .name(model.getName())
            .idxCount(model.getIndexedFieldsCount())
            .anonymous(model.isAnonymous())
            .transactionId(model.getTransactionId())
            .build();
    }
}
