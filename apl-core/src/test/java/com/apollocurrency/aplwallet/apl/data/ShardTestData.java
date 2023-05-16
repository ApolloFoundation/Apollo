/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardState;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.util.Arrays;
import java.util.List;

public class ShardTestData {
    public static final Shard SHARD_0 =
        new Shard(1L, Convert.parseHexString("8dd2cb2fcd453c53b3fe53790ac1c104a6a31583e75972ff62bced9047a15176"),
            ShardState.INIT, 2, null,
            Convert.EMPTY_LONG, Convert.EMPTY_INT, Convert.EMPTY_INT, null);
    public static final Shard SHARD_1 =
        new Shard(2L, Convert.parseHexString("a3015d38155ea3fd95fe8952f579791e4ce7f5e1e21b4ca4e0c490553d94fb7d"),
            ShardState.FULL, 3, Convert.parseHexString("a3015d38155ea3fd95fe8952f579791e4ce7f5e1e21b4ca4e0c490553d94fb7d"),
            new long[]{782179228250L, 4821792282200L, 7821792282123976600L},
            new int[]{0, 1},
            new int[]{45673250, 45673251}, Convert.parseHexString("0729528cd01d03c815e1aaf74e1c8950a411e0f20376881747e6ab667452d909"));
    public static final Shard SHARD_2 =
        new Shard(3L, Convert.parseHexString("931a8011f4ba1cdc0bcae807032fe18b1e4f0b634f8da6016e421d06c7e13693"),
            ShardState.CREATED_BY_ARCHIVE, 31, null,
            new long[]{57821792282L, 22116981092100L, 9211698109297098287L},
            new int[]{1, 1},
            new int[]{45673251, 45673252}, null);
    public static final Shard NOT_SAVED_SHARD =
        new Shard(5L, Convert.parseHexString("7a496e38973387732394ff257e73cd3e57165ed2f0ab1855d497ff6b14fd0678"),
            ShardState.FULL, 4, Convert.parseHexString("7a496e38973387732394ff257e73cd3e57165ed2f0ab1855d497ff6b14fd0678"),
            new long[]{22116981092100L, 7821792282123976600L, 9211698109297098287L},
            new int[]{1, 2},
            new int[]{45673251, 45673252}, null);
    public static final List<Shard> SHARDS = Arrays.asList(SHARD_0, SHARD_1, SHARD_2);


    public static final ShardDTO SHARD_DTO_0 =
        new ShardDTO(1L, "8dd2cb2fcd453c53b3fe53790ac1c104a6a31583e75972ff62bced9047a15176",
            0L, 2, null, null,
            Arrays.toString(new long[]{}),
            Arrays.toString(new int[]{}), Arrays.toString(new int[]{}));
    public static final ShardDTO SHARD_DTO_1 =
        new ShardDTO(2L, "a3015d38155ea3fd95fe8952f579791e4ce7f5e1e21b4ca4e0c490553d94fb7d",
            100L, 3, "a3015d38155ea3fd95fe8952f579791e4ce7f5e1e21b4ca4e0c490553d94fb7d", "0729528cd01d03c815e1aaf74e1c8950a411e0f20376881747e6ab667452d909",
            Arrays.toString(new long[]{782179228250L, 4821792282200L, 7821792282123976600L}),
            Arrays.toString(new int[]{0, 1}), Arrays.toString(new int[]{45673250, 45673251}));
    public static final ShardDTO SHARD_DTO_2 =
        new ShardDTO(3L, "931a8011f4ba1cdc0bcae807032fe18b1e4f0b634f8da6016e421d06c7e13693",
            50L, 31, null, null,
            Arrays.toString(new long[]{57821792282L, 22116981092100L, 9211698109297098287L}),
            Arrays.toString(new int[]{1, 1}), Arrays.toString(new int[]{45673251, 45673252}));
    public static final List<ShardDTO> SHARD_DTO_LIST = Arrays.asList(SHARD_DTO_0, SHARD_DTO_1, SHARD_DTO_2);

}
