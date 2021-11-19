/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.converter.db.UnconfirmedTransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.UnconfirmedTransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.UnconfirmedTransaction;
import com.google.common.cache.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemPoolTest {

    @Mock
    UnconfirmedTransactionTable table;
    @Mock
    UnconfirmedTransactionEntityToModelConverter toModelConverter;
    @Mock
    UnconfirmedTransactionModelToEntityConverter toEntityConverter;
    @Mock
    MemPoolInMemoryState state;
    @Mock
    MemPoolConfig config;


    @InjectMocks
    MemPool memPool;


    @Mock
    UnconfirmedTransaction unconfirmedTx;
    @Mock
    Transaction tx;
    @Mock
    Cache removedTxsCache;

    @Test
    void processLaterTx() {
        when(unconfirmedTx.getId()).thenReturn(1000L);

        memPool.processLater(unconfirmedTx);

        verify(removedTxsCache).invalidate(1000L);
        verify(state).processLater(unconfirmedTx);
    }


}