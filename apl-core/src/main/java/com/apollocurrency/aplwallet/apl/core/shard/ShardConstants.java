/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import javax.enterprise.inject.Vetoed;

@Vetoed
public class ShardConstants {

    public static final int DEFAULT_COMMIT_BATCH_SIZE = 100;
    public static final String BLOCK_TABLE_NAME = "block";
    public static final String TRANSACTION_TABLE_NAME = "transaction";
    public static final String SHARD_TABLE_NAME = "shard";
    public static final String BLOCK_INDEX_TABLE_NAME = "block_index";
    public static final String TRANSACTION_INDEX_TABLE_NAME = "transaction_shard_index";
    public static final String GOODS_TABLE_NAME = "goods";
    public static final String PHASING_POLL_TABLE_NAME = "phasing_poll";
    public static final Long SHARD_PERCENTAGE_FULL = 100L;

    private ShardConstants() {}
}
