/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.core.shard.ShardingScheduler;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

@Slf4j
@Singleton
public class ShardObserver {

    private final BlockchainConfig blockchainConfig;
    private final ShardingScheduler shardingScheduler;
    private final PropertiesHolder propertiesHolder;
    private final ShardService shardService;

    @Inject
    public ShardObserver(BlockchainConfig blockchainConfig,
                         PropertiesHolder propertiesHolder,
                         ShardingScheduler shardingScheduler, ShardService shardService) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder, "propertiesHolder is NULL");
        this.shardingScheduler = Objects.requireNonNull(shardingScheduler, "Sharding scheduler is NULL");
        this.shardService = shardService;
    }

    /**
     * Triggered on every block
     * @param block to be processed and sharding started when conditions are met
     */


}
