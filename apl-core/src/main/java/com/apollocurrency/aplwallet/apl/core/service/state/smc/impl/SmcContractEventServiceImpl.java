/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.api.v2.model.ContractEventDetails;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractEventLogModelToLogEntryConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractEventModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractEventLogTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractEventTable;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.aplwallet.apl.smc.events.SmcEventType;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractEvent;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractEventService;
import com.apollocurrency.aplwallet.apl.util.api.NumericRange;
import com.apollocurrency.aplwallet.apl.util.api.Range;
import com.apollocurrency.aplwallet.apl.util.api.Sort;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import com.apollocurrency.smc.contract.vm.event.EventArguments;
import com.apollocurrency.smc.data.expr.Term;
import com.apollocurrency.smc.data.jsonmapper.JsonMapper;
import com.apollocurrency.smc.data.jsonmapper.event.EventJsonMapper;
import com.apollocurrency.smc.data.type.Address;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.smc.events.SmcEventBinding.literal;
import static com.apollocurrency.smc.util.HexUtils.toHex;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcContractEventServiceImpl implements SmcContractEventService {

    private final Blockchain blockchain;
    private final SmcContractEventTable contractEventTable;
    private final SmcContractEventLogTable contractEventLogTable;
    private final ContractEventLogModelToLogEntryConverter logEntryConverter;
    private final ContractEventModelToEntityConverter entityConverter;
    private final HashSumProvider hashSumProvider;
    private final Event<AplContractEvent> cdiContractEvent;
    private final SmcContractRepository smcContractRepository;
    private final JsonMapper jsonMapper;

    @Inject
    public SmcContractEventServiceImpl(Blockchain blockchain, SmcContractEventTable contractEventTable,
                                       SmcContractEventLogTable contractEventLogTable,
                                       ContractEventLogModelToLogEntryConverter contractEventLogModelToLogEntryConverter,
                                       ContractEventModelToEntityConverter contractEventModelToEntityConverter,
                                       HashSumProvider hashSumProvider,
                                       Event<AplContractEvent> cdiContractEvent,
                                       SmcContractRepository smcContractRepository) {
        this.blockchain = blockchain;
        this.contractEventTable = contractEventTable;
        this.contractEventLogTable = contractEventLogTable;
        this.logEntryConverter = contractEventLogModelToLogEntryConverter;
        this.entityConverter = contractEventModelToEntityConverter;
        this.hashSumProvider = hashSumProvider;
        this.cdiContractEvent = cdiContractEvent;
        this.smcContractRepository = smcContractRepository;
        this.jsonMapper = new EventJsonMapper();
    }

    @Override
    public boolean isContractExist(Address contract) {
        return smcContractRepository.isContractExist(contract);
    }

    @Override
    @Transactional
    public void saveEvent(AplContractEvent event) {
        var eventEntity = entityConverter.convert(event);
        eventEntity.setHeight(blockchain.getHeight());
        if (eventEntity.getHeight() != event.getHeight()) {
            var errMsg = String.format("Different height value: blockchain height=%d, event height=%d", eventEntity.getHeight(), event.getHeight());
            log.error(errMsg);
            throw new IllegalStateException(errMsg);
        }
        var logEntry = logEntryConverter.convert(event);
        logEntry.setHeight(blockchain.getHeight());

        log.debug("Save contract event at height {}, event={}", eventEntity.getHeight(), eventEntity.getLogInfo());
        log.debug("Check if exists, contract={} name={} signature={}", eventEntity.getContract(), eventEntity.getName(), toHex(eventEntity.getSignature()));
        var oldEvent = contractEventTable.get(contractEventTable.getDbKeyFactory().newKey(eventEntity));
        if (oldEvent == null || oldEvent.isNew()) {
            log.debug("Insert new event contract={} name={} signature={}", eventEntity.getContract(), eventEntity.getName(), toHex(eventEntity.getSignature()));
            //save the new contract event type
            contractEventTable.insert(eventEntity);
        }
        log.debug("Insert contract event log at height {}, log={}", eventEntity.getHeight(), logEntry.getLogInfo());
        contractEventLogTable.insert(logEntry);
    }

    @Override
    public void fireCdiEvent(AplContractEvent event) {
        cdiContractEvent.select(literal(SmcEventType.EMIT_EVENT)).fireAsync(event);
    }

    @Override
    public List<ContractEventDetails> getEventsByFilter(Long contract, String eventName, Term predicate, Range range, Range paging, Sort order) {
        var height = blockchain.getHeight();
        int fromBlock = (range.from().intValue() < 0) ? height : range.from().intValue();
        int toBlock = (range.to().intValue() < 0) ? height : range.to().intValue();
        var blockRange = new NumericRange(fromBlock, toBlock);
        log.trace("getEventsByFilter: height={} contract={} eventName={} blockRange={} paging={} order={}", height, contract, eventName, blockRange, paging, order);
        var result = contractEventLogTable.getEventsByFilter(contract, eventName, blockRange, paging, order);
        log.trace("getEventsByFilter: resultSet.size={}", result.size());
        //filter result set
        var deserializer = jsonMapper.deserializer();
        var rc = result.stream().filter(event -> {
            var args = deserializer.deserialize(event.getState(), EventArguments.class);
            return predicate.test(args.getMap());
        }).collect(Collectors.toList());
        log.trace("getEventsByFilter: apply filter={} result.size={}", predicate, rc.size());
        return rc;
    }
}
