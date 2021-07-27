/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.txlog;

import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.smc.txlog.RecordProcessor;
import com.apollocurrency.smc.txlog.RecordType;
import com.apollocurrency.smc.txlog.TxLog;
import com.apollocurrency.smc.txlog.TxLogProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class SmcTxLogProcessor implements TxLogProcessor {
    private final AccountService accountService;

    public SmcTxLogProcessor(AccountService accountService) {
        this.accountService = Objects.requireNonNull(accountService);
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
        if (rType.equals(SmcRecordType.TRANSFER)) {
            return createTransferRecordProcessor();
        }
        throw new IllegalArgumentException("Unsupported record type:" + rType);
    }

    private RecordProcessor<TransferRecord> createTransferRecordProcessor() {
        return (header, data) -> {
            var sender = accountService.getAccount(data.getSender());
            var recipient = accountService.getAccount(data.getRecipient());
            accountService.addToBalanceAndUnconfirmedBalanceATM(sender, data.getEvent(), data.getTransaction(), -data.getValue());
            accountService.addToBalanceAndUnconfirmedBalanceATM(recipient, data.getEvent(), data.getTransaction(), data.getValue());
        };
    }

}
