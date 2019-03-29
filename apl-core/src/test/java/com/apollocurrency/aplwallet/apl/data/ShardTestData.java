package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.util.Arrays;
import java.util.List;

public class ShardTestData {
    public static final Shard SHARD_0 = new Shard(1L, Convert.parseHexString("8dd2cb2fcd453c53b3fe53790ac1c104a6a31583e75972ff62bced9047a15176"));
    public static final Shard SHARD_1 = new Shard(2L, Convert.parseHexString("a3015d38155ea3fd95fe8952f579791e4ce7f5e1e21b4ca4e0c490553d94fb7d"));
    public static final Shard SHARD_2 = new Shard(3L, Convert.parseHexString("931a8011f4ba1cdc0bcae807032fe18b1e4f0b634f8da6016e421d06c7e13693"));
    public static final Shard NOT_SAVED_SHARD = new Shard(4L, Convert.parseHexString("7a496e38973387732394ff257e73cd3e57165ed2f0ab1855d497ff6b14fd0678"));
    public static final List<Shard> SHARDS = Arrays.asList(SHARD_0, SHARD_1, SHARD_2);
}
