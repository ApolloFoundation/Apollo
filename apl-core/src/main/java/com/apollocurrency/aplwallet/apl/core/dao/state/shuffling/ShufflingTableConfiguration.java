/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.files.shards.ShardPresentData;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Singleton
public class ShufflingTableConfiguration {
    private final TaskDispatchManager taskManager;
    private final ShufflingTable shufflingTable;
    private InMemoryShufflingRepository inMemRepo;
    @Getter
    private final boolean cacheEnabled;

    @Inject
    public ShufflingTableConfiguration(TaskDispatchManager taskManager,
                                       ShufflingTableProducer shufflingTableProducer,
                                       @Property(name = "apl.enableShufflingMemTable") boolean cacheEnabled) {
        this.shufflingTable = shufflingTableProducer.shufflingTable();
        this.taskManager = taskManager;
        this.cacheEnabled = cacheEnabled;
    }

    @PostConstruct
    void init() {
        if (isCacheEnabled()) {
            log.info("'Shuffling cache' is TURNED ON...");
            inMemRepo = new InMemoryShufflingRepository();
            try {
                warmUp();
            } catch (SQLException e) {
                throw new RuntimeException("Shuffling in-memory cached table warm up error: ", e);
            }

            TaskDispatcher taskDispatcher = taskManager.newScheduledDispatcher("ShufflingTableConfiguration-periodics");
            taskDispatcher.schedule(Task.builder()
                .name("Shuffling in-memory cache health check")
                .initialDelay(30_000)
                .delay(300_000)
                .task(() -> {
                    int dbCount = shufflingTable.getRowCount();
                    int memCount = inMemRepo.rowCount();
                    log.info("Shuffling stats: in db {}, in mem {}", dbCount, memCount);
                })
                .build());
        } else {
            log.info("Shuffling cache is TURNED OFF...");
        }
    }

    public void onShardImported(@Observes ShardPresentData shardPresentData) {
        try {
            warmUp();
        } catch (SQLException e) {
            log.error("Unable to warmup in-memory shuffling cache after successful shard " + shardPresentData.getShardId() + " import", e);
        }
    }

    private void warmUp() throws SQLException {
        log.info("Warming up Shuffling cache");
        List<Shuffling> allShufflings = shufflingTable.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        inMemRepo.putAll(allShufflings);
        log.info("Shuffling cache warm up is done with {} shufflings", allShufflings.size());
    }

    @Produces
    @Singleton
    public ShufflingRepository getTable() {
        if (isCacheEnabled()) {
            return new ShufflingCachedTable(inMemRepo, shufflingTable);
        } else {
            return shufflingTable;
        }
    }
}