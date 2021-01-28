/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.Data;

import javax.inject.Singleton;

@Data
@Singleton
public class ShardSchedulingConfig {
    private static final int DEFAULT_MIN_DELAY = 10; // minutes
    private static final int DEFAULT_MAX_DELAY = 60; // minutes

    private final int minDelay;
    private final int maxDelay;
    private final int maxRollback;
    private final boolean createShards;



    public ShardSchedulingConfig(@Property(name = "apl.shard.minDelay", defaultValue = "" + DEFAULT_MIN_DELAY) int minDelay,
                                 @Property(name = "apl.shard.maxDelay", defaultValue = ""  + DEFAULT_MAX_DELAY) int maxDelay,
                                 @Property(name = "apl.noshardcreate", defaultValue = "false") boolean noShardCreate,
                                 @Property(name = "apl.maxRollback", defaultValue = "" + PropertiesHolder.DEFAULT_MAX_ROLLBACK) int maxRollback
                                 ) {
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        this.maxRollback = maxRollback;
        this.createShards = !noShardCreate;
    }
}
