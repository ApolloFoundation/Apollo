/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.TransactionDbInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;


class ExcludedTransactionDbIdExtractorTest {
    PhasingPollService phasingPollService = mock(PhasingPollService.class);
    Blockchain blockchain = mock(Blockchain.class);
    ExcludedTransactionDbIdExtractor extractor = new ExcludedTransactionDbIdExtractor(phasingPollService, blockchain);

    @Test
    void testGetExcludeInfo() {
        doReturn(List.of(new TransactionDbInfo(1, 2), new TransactionDbInfo(2, 3))).when(blockchain).getTransactionsBeforeHeight(100);
        doReturn(List.of(new TransactionDbInfo(2, 3), new TransactionDbInfo(3, 4))).when(phasingPollService).getActivePhasedTransactionDbInfoAtHeight(100);

        ExcludeInfo excludeInfo = extractor.getExcludeInfo(100);

        assertEquals(Set.of(3L, 2L), excludeInfo.getExportDbIds());
        assertEquals(Set.of(1L, 2L), excludeInfo.getNotCopyDbIds());
        assertEquals(Set.of(2L, 3L), excludeInfo.getNotDeleteDbIds());

        verify(blockchain, only()).getTransactionsBeforeHeight(100);
        verify(phasingPollService, only()).getActivePhasedTransactionDbInfoAtHeight(100);

    }
}