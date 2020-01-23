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
    public static final String ACCOUNT_TABLE_NAME = "account";
    public static final String BLOCK_INDEX_TABLE_NAME = "block_index";
    public static final String TRANSACTION_INDEX_TABLE_NAME = "transaction_shard_index";
    public static final String GOODS_TABLE_NAME = "goods";
    public static final String PHASING_POLL_TABLE_NAME = "phasing_poll";
    public static final String DB_BACKUP_FORMAT = "BACKUP-BEFORE-%s.zip";
    public static final String TAGGED_DATA_TABLE_NAME = "tagged_data";
    public static final String DATA_TAG_TABLE_NAME = "data_tag";
    public static final String UNCONFIRMED_TX_TABLE_NAME = "unconfirmed_transaction";
    public static final String GENESIS_PK_TABLE_NAME = "genesis_public_key";
    public static final String DEX_ORDER_TABLE_NAME = "dex_offer";
    public static final String ACCOUNT_CURRENCY_TABLE_NAME = "account_currency";
    public static final String ACCOUNT_ASSET_TABLE_NAME = "account_asset";


    private ShardConstants() {}
}
