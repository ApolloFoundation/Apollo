package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEventLogEntry;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractEvent;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

import javax.inject.Singleton;

@Singleton
public class ContractEventLogModelToLogEntryConverter implements Converter<AplContractEvent, SmcContractEventLogEntry> {
    @Override
    public SmcContractEventLogEntry apply(AplContractEvent model) {
        return SmcContractEventLogEntry.builder()
            .eventId(model.getId())
            .signature(model.getSignature())
            .transactionId(new AplAddress(model.getTransaction()).getLongId())
            .state(model.getState())
            .build();
    }
}
