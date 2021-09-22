/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.event;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.txlog.EventLogRecord;
import com.apollocurrency.aplwallet.apl.crypto.AplIdGenerator;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractEvent;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import com.apollocurrency.smc.contract.vm.event.SmcContractEventManager;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.ContractEvent;
import com.apollocurrency.smc.txlog.TxLog;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */

@Slf4j
public class AplContractEventManager extends SmcContractEventManager {

    public AplContractEventManager(Address contract, Address transaction, HashSumProvider hashSumProvider, TxLog txLog) {
        super(contract, transaction, hashSumProvider, txLog);
    }

    @Override
    public void emit(ContractEvent event) {
        var eventId = generateId(event.getTxIdx(), event.getSignature());
        log.debug("Generate event_id={} event={}", eventId, event);

        var smcEvent = AplContractEvent.builder()
            .event(event)
            .id(eventId)
            .contractId(new AplAddress(getContract()).getLongId())
            .transactionId(new AplAddress(getTransaction()).getLongId())
            .build();

        var rec = EventLogRecord.builder()
            .event(smcEvent)
            .build();

        txLogger().append(rec);
    }

    protected long generateId(int eventIdx, byte[] eventSignature) {
        var hash = getHashSumProvider().sha256()
            .update(getContract().key())
            .update(BigInteger.valueOf(eventIdx).toByteArray())
            .digest(eventSignature);

        return AplIdGenerator.ACCOUNT.getIdByHash(hash).longValue();
    }

}
