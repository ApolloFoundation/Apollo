/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.shard.ShardingScheduler;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Setter
public class ShardingInitTaskBackgroundScheduler {
    private static  final String BACKGROUND_SERVICE_NAME = "ShardingInitDispatcher";
    @Inject
    ShardingScheduler shardingScheduler;
    @Inject
    Blockchain blockchain;
    @Inject
    TrimService trimService;
    @Inject
    TaskDispatchManager taskDispatchManager;

    @PostConstruct
    public void init() {
        TaskDispatcher dispatcher = taskDispatchManager.newBackgroundDispatcher(BACKGROUND_SERVICE_NAME);
        Task shardingInitTask = Task.builder()
            .name("ShardingInitTask")
            .task(() -> {
                shardingScheduler.init(blockchain.getHeight(), trimService.getLastTrimHeight());
            })
            .delay(10000)
            .initialDelay(10000)
            .build();
        dispatcher.invokeAfter(shardingInitTask);
    }
}
