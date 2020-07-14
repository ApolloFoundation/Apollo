/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.shard.model.ExcludeInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;


class ExcludedTransactionDbIdExtractorTest {
    PhasingPollService phasingPollService = mock(PhasingPollService.class);
    Blockchain blockchain = mock(Blockchain.class);
    ExcludedTransactionDbIdExtractor extractor = new ExcludedTransactionDbIdExtractor(phasingPollService, blockchain);

    @Test
    void testGetExcludeInfo() {
        doReturn(List.of(new TransactionDbInfo(1, 2), new TransactionDbInfo(2, 3), new TransactionDbInfo(5, 10))).when(blockchain).getTransactionsBeforeHeight(100);
        doReturn(List.of(new TransactionDbInfo(2, 3), new TransactionDbInfo(3, 4))).when(phasingPollService).getActivePhasedTransactionDbInfoAtHeight(120);

        ExcludeInfo excludeInfo = extractor.getExcludeInfo(100, 120);

        assertEquals(Set.of(3L, 2L), excludeInfo.getExportDbIds());
        assertEquals(Set.of(1L, 2L, 5L), excludeInfo.getNotCopyDbIds());
        assertEquals(Set.of(2L, 3L), excludeInfo.getNotDeleteDbIds());

        verify(blockchain, only()).getTransactionsBeforeHeight(100);
        verify(phasingPollService, only()).getActivePhasedTransactionDbInfoAtHeight(120);

    }

    @Test
    void testGetExcludeInfoWhenStartHeightEqualToFinishHeight() {
        assertThrows(IllegalArgumentException.class, () -> extractor.getExcludeInfo(100, 100));
    }
}