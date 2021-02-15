/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.Getter;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Singleton;

@Getter
@Setter
@Singleton
public class
ShardSchedulingConfig {
    private static final int DEFAULT_MIN_DELAY = 600; // seconds
    private static final int DEFAULT_MAX_DELAY = 3600; // seconds

    private final int minDelay;
    private final int maxDelay;
    private final int maxRollback;
    private final boolean createShards;



    @Inject
    public ShardSchedulingConfig(@Property(name = "apl.shard.minDelay", defaultValue = "" + DEFAULT_MIN_DELAY) int minDelay,
                                 @Property(name = "apl.shard.maxDelay", defaultValue = ""  + DEFAULT_MAX_DELAY) int maxDelay,
                                 @Property(name = "apl.noshardcreate", defaultValue = "false") boolean noShardCreate,
                                 @Property(name = "apl.maxRollback", defaultValue = "" + Constants.MAX_AUTO_ROLLBACK) int maxRollback
                                 ) {
        if (maxDelay <= minDelay) {
            throw new IllegalArgumentException("Sharding max delay should be greater than min delay, but got minDelay = " + minDelay + ", maxDelay = " + maxDelay);
        }
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        this.maxRollback = Math.max(maxRollback, Constants.MAX_AUTO_ROLLBACK);
        this.createShards = !noShardCreate;
    }

    public boolean shardDelayed() {
        return minDelay >= 0 && maxDelay > 0;
    }
}
