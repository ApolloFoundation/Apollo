/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class ActiveGenerators {
    private final Logger LOG = LoggerFactory.getLogger(ActiveGenerators.class);
    private final int MAX_TRACKED_GENERATORS = 50;
    /**
     * Active block generators
     */
    private final Set<Long> activeGeneratorIds = new HashSet<>();
    /**
     * Sorted list of generators for the next block
     */
    private final List<ActiveGenerator> activeGenerators = new ArrayList<>();
    private Blockchain blockchain;
    /**
     * Active block identifier
     */
    private long activeBlockId;
    /**
     * Generator list has been initialized
     */
    private boolean generatorsInitialized = false;

    @Inject
    public ActiveGenerators(Blockchain blockchain) {
        this.blockchain = blockchain;
    }


    @PostConstruct
    public void init() {
        if (!generatorsInitialized) {
            activeGeneratorIds.addAll(blockchain.getBlockGenerators(MAX_TRACKED_GENERATORS));
            activeGeneratorIds.forEach(activeGeneratorId -> activeGenerators.add(new ActiveGenerator(activeGeneratorId)));
            LOG.debug(activeGeneratorIds.size() + " block generators found");
            generatorsInitialized = true;
        } else {
            throw new IllegalStateException("Active generators already initialized");
        }
    }

    /**
     * Return a list of generators for the next block.  The caller must hold the blockchain
     * read lock to ensure the integrity of the returned list.
     *
     * @return List of generator account identifiers
     */
    public synchronized List<ActiveGenerator> getNextGenerators() {
        List<ActiveGenerator> generatorList;
        if (!generatorsInitialized) {
            throw new IllegalStateException("Active generators not yet initialized");
        }
        long blockId = blockchain.getLastBlock().getId();
        if (blockId != activeBlockId) {
            activeBlockId = blockId;
            Block lastBlock = blockchain.getLastBlock();
            for (ActiveGenerator generator : activeGenerators) {
                generator.setLastBlock(lastBlock);
            }
            Collections.sort(activeGenerators);
        }
        generatorList = new ArrayList<>(activeGenerators);
        return generatorList;

    }

    public synchronized void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        long generatorId = block.getGeneratorId();
        synchronized (activeGenerators) {
            if (!activeGeneratorIds.contains(generatorId)) {
                activeGeneratorIds.add(generatorId);
                activeGenerators.add(new ActiveGenerator(generatorId));
            }
        }
    }
}