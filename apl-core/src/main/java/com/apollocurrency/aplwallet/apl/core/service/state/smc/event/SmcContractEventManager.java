/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.event;

import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractEventService;
import com.apollocurrency.aplwallet.apl.crypto.AplIdGenerator;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.smc.contract.ContractException;
import com.apollocurrency.smc.contract.vm.ContractEvent;
import com.apollocurrency.smc.contract.vm.ContractEventManager;
import com.apollocurrency.smc.data.json.JsonMapper;
import com.apollocurrency.smc.data.json.SmcJsonMapper;
import com.apollocurrency.smc.data.type.Address;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */

@Slf4j
public class SmcContractEventManager implements ContractEventManager {
    private final AplAddress contract;
    private final AplAddress transaction;
    private final SmcContractEventService contractEventService;
    private final AtomicInteger eventIdx;//the sequential index of the event within the blockchain transaction.

    private final JsonMapper jsonMapper;


    public SmcContractEventManager(Address contract, Address transaction, SmcContractEventService contractEventService) {
        this.contract = new AplAddress(contract);
        this.transaction = new AplAddress(transaction);
        this.contractEventService = contractEventService;
        this.eventIdx = new AtomicInteger(0);
        this.jsonMapper = new SmcJsonMapper();
    }

    @Override
    public void emit(ContractEvent event) {
        var signature = generateSignature(event);
        var txIdx = eventIdx.getAndIncrement();
        var eventId = generateId(txIdx, signature);
        var state = serializeParameters(event.getParams());

        var smcEvent = SmcContractEvent.builder()
            .contract(contract.getLongId())
            .id(eventId)
            .signature(signature)
            .name(event.getName())
            .idxCount(event.getIndexedFieldsCount())
            .anonymous(event.isAnonymous())
            .transactionId(transaction.getLongId())
            .txIdx(txIdx)
            .entry(state)
            .build();

        contractEventService.saveEvent(smcEvent);


    }

    private long generateId(int eventIdx, byte[] eventSignature) {
        var md = Crypto.sha256();
        md.update(transaction.key());
        var hash = md.digest(eventSignature);
        return AplIdGenerator.ACCOUNT.getIdByHash(hash).longValue();
    }

    /**
     * Returns event signature hash.
     * The signature consists of three parts name, parameters count, and anonymous flag that delimited by a colon.
     *
     * @param event the given contract event
     * @return event signature hash
     */
    private byte[] generateSignature(ContractEvent event) {
        return Crypto.sha256().digest(event.getFullName().getBytes(StandardCharsets.UTF_8));
    }

    private String serializeParameters(Object... parameters) {
        var objectMapper = jsonMapper.serializer();
        String jsonObject;
        try {
            jsonObject = objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            throw new ContractException(contract, "Can't Serialize event parameters" + Arrays.toString(parameters) + ". Cause: " + e.getMessage(), e);
        }
        return jsonObject;
    }

}
