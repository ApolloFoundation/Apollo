/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.db.dao.model.BlockIndex;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.TransactionIndex;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.util.Arrays;
import java.util.List;

public class IndexTestData {
    public static final BlockIndex BLOCK_INDEX_0 = new BlockIndex(3L, 3L, 30);
    public static final BlockIndex BLOCK_INDEX_1 = new BlockIndex(1L, 1L, 1);
    public static final BlockIndex BLOCK_INDEX_2 = new BlockIndex(2L, 2L, 2);
    public static final BlockIndex NOT_SAVED_BLOCK_INDEX = new BlockIndex(2L, 100L, 3);
    public static final TransactionIndex TRANSACTION_INDEX_0 = new TransactionIndex(100L  ,Convert.parseHexString("cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e") ,BLOCK_INDEX_0.getBlockId());
    public static final TransactionIndex TRANSACTION_INDEX_1 = new TransactionIndex(101L  ,Convert.parseHexString("2270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d") ,BLOCK_INDEX_1.getBlockId());
    public static final TransactionIndex TRANSACTION_INDEX_2 = new TransactionIndex(102L  ,Convert.parseHexString("b96d5e9f64e51c597513717691eeeeaf18a26a864034f62c") ,BLOCK_INDEX_1.getBlockId());
    public static final TransactionIndex TRANSACTION_INDEX_3 = new TransactionIndex(103L  ,Convert.parseHexString("cca5a1f825f9b918be00f35406f70b108b6656b299755558") ,BLOCK_INDEX_1.getBlockId());
    public static final TransactionIndex NOT_SAVED_TRANSACTION_INDEX_0 = new TransactionIndex(200L, Convert.parseHexString("fc18147b1d9360b4ae06fc65905948fbce127c302201e9a1"), BLOCK_INDEX_2.getBlockId());
    public static final TransactionIndex NOT_SAVED_TRANSACTION_INDEX_1 = new TransactionIndex(201L, Convert.parseHexString("3fc7b055930adb2997b5fffaaa2cf86fa360fe235311e9d3"), BLOCK_INDEX_2.getBlockId());
    public static final List<TransactionIndex> TRANSACTION_INDEXES = Arrays.asList(TRANSACTION_INDEX_0, TRANSACTION_INDEX_1, TRANSACTION_INDEX_2, TRANSACTION_INDEX_3);
    public static final List<BlockIndex> BLOCK_INDEXES = Arrays.asList(BLOCK_INDEX_0, BLOCK_INDEX_1, BLOCK_INDEX_2);
}
