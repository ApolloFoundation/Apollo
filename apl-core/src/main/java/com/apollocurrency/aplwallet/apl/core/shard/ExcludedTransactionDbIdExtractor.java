/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.shard.model.ExcludeInfo;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ExcludedTransactionDbIdExtractor {
    private final PhasingPollService phasingPollService;
    private final Blockchain blockchain;

    @Inject
    public ExcludedTransactionDbIdExtractor(PhasingPollService phasingPollService, Blockchain blockchain) {
        this.phasingPollService = phasingPollService;
        this.blockchain = blockchain;
    }

    public ExcludeInfo getExcludeInfo(int startHeight, int finishHeight) {
        log.debug("get Info: start={}, finish={}", startHeight, finishHeight);
        if (startHeight >= finishHeight) {
            throw new IllegalArgumentException("startHeight should be less than finish height but got start=" + startHeight + " and finish" + finishHeight);
        }
        List<TransactionDbInfo> activePhasedTransactions = phasingPollService.getActivePhasedTransactionDbInfoAtHeight(finishHeight);
        log.trace("get activePhasedTransactions: {}", activePhasedTransactions);
        List<TransactionDbInfo> transactionsBeforeHeight = blockchain.getTransactionsBeforeHeight(startHeight);
        log.trace("get transactionsBeforeHeight: {}", transactionsBeforeHeight);
        List<TransactionDbInfo> deleteNotExportNotCopy = transactionsBeforeHeight.stream().filter(tdi -> !activePhasedTransactions.contains(tdi)).collect(Collectors.toList());
        log.trace("get deleteNotExportNotCopy: {}", deleteNotExportNotCopy);
        List<TransactionDbInfo> notDeleteExportNotCopy = transactionsBeforeHeight.stream().filter(activePhasedTransactions::contains).collect(Collectors.toList());
        log.trace("get notDeleteExportNotCopy: {}", notDeleteExportNotCopy);
        List<TransactionDbInfo> notDeleteExportCopy = activePhasedTransactions.stream().filter(e -> !notDeleteExportNotCopy.contains(e)).collect(Collectors.toList());
        log.trace("get notDeleteExportCopy: {}", notDeleteExportCopy);
        return new ExcludeInfo(deleteNotExportNotCopy, notDeleteExportNotCopy, notDeleteExportCopy);
    }
}
