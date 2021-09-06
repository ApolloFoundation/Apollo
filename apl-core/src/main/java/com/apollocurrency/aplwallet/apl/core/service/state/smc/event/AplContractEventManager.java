/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.event;

import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplContractEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractEventService;
import com.apollocurrency.aplwallet.apl.crypto.AplIdGenerator;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import com.apollocurrency.smc.contract.vm.event.SmcContractEventManager;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.ContractEvent;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */

@Slf4j
public class AplContractEventManager extends SmcContractEventManager {

    private final SmcContractEventService contractEventService;

    public AplContractEventManager(Address contract, Address transaction, HashSumProvider hashSumProvider, SmcContractEventService contractEventService) {
        super(contract, transaction, hashSumProvider);
        this.contractEventService = contractEventService;
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

        contractEventService.saveEvent(smcEvent);
    }

    protected long generateId(int eventIdx, byte[] eventSignature) {
        var md = getHashSumProvider().sha256();
        md.update(getTransaction().key());
        md.update(BigInteger.valueOf(eventIdx).toByteArray());
        var hash = md.digest(eventSignature);
        return AplIdGenerator.ACCOUNT.getIdByHash(hash).longValue();
    }

}
