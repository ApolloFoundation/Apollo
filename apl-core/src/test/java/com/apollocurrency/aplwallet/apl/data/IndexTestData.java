package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.db.dao.model.BlockIndex;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.TransactionIndex;

import java.util.Arrays;
import java.util.List;

public class IndexTestData {
    public static final BlockIndex BLOCK_INDEX_0 = new BlockIndex(10L, 30L, 30);
    public static final BlockIndex BLOCK_INDEX_1 = new BlockIndex(1L, 1L, 1);
    public static final BlockIndex BLOCK_INDEX_2 = new BlockIndex(2L, 2L, 2);
    public static final BlockIndex NOT_SAVED_BLOCK_INDEX = new BlockIndex(2L, 100L, 3);
    public static final TransactionIndex TRANSACTION_INDEX_0 = new TransactionIndex(100L, 30L);
    public static final TransactionIndex TRANSACTION_INDEX_1 = new TransactionIndex(101L, 1L);
    public static final TransactionIndex TRANSACTION_INDEX_2 = new TransactionIndex(102L, 1L);
    public static final TransactionIndex TRANSACTION_INDEX_3 = new TransactionIndex(103L, 1L);
    public static final TransactionIndex NOT_SAVED_TRANSACTION_INDEX_0 = new TransactionIndex(200L, 2L);
    public static final TransactionIndex NOT_SAVED_TRANSACTION_INDEX_1 = new TransactionIndex(201L, 2L);
    public static final List<TransactionIndex> TRANSACTION_INDEXES = Arrays.asList(TRANSACTION_INDEX_0, TRANSACTION_INDEX_1, TRANSACTION_INDEX_2, TRANSACTION_INDEX_3);
    public static final List<BlockIndex> BLOCK_INDEXES = Arrays.asList(BLOCK_INDEX_0, BLOCK_INDEX_1, BLOCK_INDEX_2);
}
