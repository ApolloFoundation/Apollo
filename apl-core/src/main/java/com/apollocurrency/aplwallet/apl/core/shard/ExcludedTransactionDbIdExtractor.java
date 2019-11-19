/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.shard.model.ExcludeInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

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
        log.trace("ExcludeInfo ==== startHeight={} finishHeight={}", startHeight, finishHeight);
        if (startHeight >= finishHeight) {
            throw new IllegalArgumentException("startHeight should be less than finish height but got start=" + startHeight + " and finish" + finishHeight);
        }
        List<TransactionDbInfo> activePhasedTransactions = phasingPollService.getActivePhasedTransactionDbInfoAtHeight(finishHeight);
        List<TransactionDbInfo> transactionsBeforeHeight = blockchain.getTransactionsBeforeHeight(startHeight);
        List<TransactionDbInfo> deleteNotExportNotCopy = transactionsBeforeHeight.stream().filter(tdi->!activePhasedTransactions.contains(tdi)).collect(Collectors.toList());
        List<TransactionDbInfo> notDeleteExportNotCopy = transactionsBeforeHeight.stream().filter(activePhasedTransactions::contains).collect(Collectors.toList());
        List<TransactionDbInfo> notDeleteExportCopy = activePhasedTransactions.stream().filter(e -> !notDeleteExportNotCopy.contains(e)).collect(Collectors.toList());
        return new ExcludeInfo(deleteNotExportNotCopy, notDeleteExportNotCopy, notDeleteExportCopy);
    }
}
