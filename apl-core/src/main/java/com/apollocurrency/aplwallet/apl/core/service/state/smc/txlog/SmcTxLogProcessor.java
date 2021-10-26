/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.txlog;

import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractEvent;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractEventService;
import com.apollocurrency.smc.txlog.Record;
import com.apollocurrency.smc.txlog.RecordProcessor;
import com.apollocurrency.smc.txlog.RecordType;
import com.apollocurrency.smc.txlog.TxLog;
import com.apollocurrency.smc.txlog.TxLogProcessor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcTxLogProcessor implements TxLogProcessor {
    private final AccountService accountService;
    private final SmcContractEventService contractEventService;

    private Map<RecordType, RecordProcessor<? extends Record>> processorMap;

    @Inject
    public SmcTxLogProcessor(AccountService accountService, SmcContractEventService contractEventService) {
        this.accountService = Objects.requireNonNull(accountService);
        this.contractEventService = Objects.requireNonNull(contractEventService);
        init();
    }

    private void init() {
        processorMap = new HashMap<>();
        processorMap.put(SmcRecordType.SEND_MONEY, createTransferRecordProcessor());
        processorMap.put(SmcRecordType.FIRE_EVENT, createEventLogRecordProcessor());
    }

    @Override
    public void process(TxLog txLog) {
        var iterator = txLog.read(0);
        while (iterator.next()) {
            var record = iterator.getRecord();
            var proc = createRecordProcessor(record.type());
            proc.process(iterator.getHeader(), record);
        }
    }

    @Override
    public RecordProcessor createRecordProcessor(RecordType rType) {
        var proc = processorMap.get(rType);
        if (proc == null) {
            throw new IllegalArgumentException("Unsupported record type:" + rType);
        }
        return proc;
    }

    private RecordProcessor<SendMoneyRecord> createTransferRecordProcessor() {
        return (header, data) -> {
            var sender = accountService.getAccount(data.getSender());
            var recipient = accountService.getAccount(data.getRecipient());
            accountService.addToBalanceAndUnconfirmedBalanceATM(sender, data.getEvent(), data.getTransaction(), -data.getValue());
            accountService.addToBalanceAndUnconfirmedBalanceATM(recipient, data.getEvent(), data.getTransaction(), data.getValue());
            log.debug("Apply the transferring command, contract={} sender={} recipient={} amount={}", data.getContract(), data.getSender(), data.getRecipient(), data.getValue());
        };
    }

    private RecordProcessor<EventLogRecord> createEventLogRecordProcessor() {
        return (header, data) -> {
            final AplContractEvent event = data.getEvent();
            contractEventService.saveEvent(event);
            contractEventService.fireCdiEvent(event);
            log.debug("Apply the firing event command, contract={} event={}", event.getContract(), event);
        };
    }

}
