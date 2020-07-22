/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata.impl;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.apollocurrency.aplwallet.apl.core.app.runnable.GenerateBlocksThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.GeneratorEntity;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Vetoed // temporary
public class GeneratorServiceImpl {

    private static final String BACKGROUND_SERVICE_NAME = "GeneratorService";
//    private static final byte[] fakeForgingPublicKey;

//    private static final Listeners<Generator, Generator.GeneratorEvent> listeners = new Listeners<>();
    private static final ConcurrentMap<Long, GeneratorEntity> generators = new ConcurrentHashMap<>();
    private static final Collection<GeneratorEntity> allGenerators = Collections.unmodifiableCollection(generators.values());
    private final PropertiesHolder propertiesHolder;
    private final int MAX_FORGERS;
    private final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;
    private final GlobalSync globalSync;
    private BlockchainProcessor blockchainProcessor;
    private final TransactionProcessor transactionProcessor;
    private volatile TimeService timeService;
    private final AccountService accountService;
    private final TaskDispatchManager taskDispatchManager;
    private static volatile boolean suspendForging = false;
//    private static volatile List<GeneratorEntity> sortedForgers = null;
    private long lastBlockId;
//    private int delayTime = propertiesHolder.FORGING_DELAY();
    private GenerateBlocksThread generateBlocksThread;

    public GeneratorServiceImpl(PropertiesHolder propertiesHolder,
                                BlockchainConfig blockchainConfig,
                                Blockchain blockchain,
                                GlobalSync globalSync,
                                TransactionProcessor transactionProcessor,
                                TimeService timeService,
                                AccountService accountService,
                                TaskDispatchManager taskDispatchManager) {
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder);
        this.blockchainConfig = blockchainConfig;
        this.blockchain = blockchain;
        this.globalSync = globalSync;
        this.transactionProcessor = transactionProcessor;
        this.timeService = timeService;
        this.accountService = accountService;
        this.taskDispatchManager = taskDispatchManager;
        this.MAX_FORGERS = propertiesHolder.getIntProperty("apl.maxNumberOfForgers");

        if (!propertiesHolder.isLightClient()) {
            generateBlocksThread = new GenerateBlocksThread(this.propertiesHolder, this.globalSync,
                this.blockchain, this.blockchainConfig, this.timeService,
                this.transactionProcessor, this.accountService, this);
            taskDispatchManager.newBackgroundDispatcher(BACKGROUND_SERVICE_NAME)
                .schedule(Task.builder()
                    .name("GenerateBlocks")
                    .delay(500)
                    .task(generateBlocksThread)
                    .build());
        }

    }

    public GeneratorEntity startForging(byte[] keySeed) {
        if (generators.size() >= MAX_FORGERS) {
            throw new RuntimeException("Cannot forge with more than " + MAX_FORGERS + " accounts on the same node");
        }
        byte[] publicKey = Crypto.getPublicKey(keySeed);
        long accountId = AccountService.getId(publicKey);
        GeneratorEntity generator = new GeneratorEntity(keySeed, publicKey, accountId);
        globalSync.updateLock();
        try {
            if (blockchain.getHeight() >= blockchainConfig.getLastKnownBlock()) {
                generateBlocksThread.setLastBlock(blockchain.getLastBlock(), generator);
            }
//            sortedForgers = null;
            generateBlocksThread.resetSortedForgers();
        } finally {
            globalSync.updateUnlock();
        }

        GeneratorEntity old = generators.putIfAbsent(generator.getAccountId(), generator);
        if (old != null) {
            log.debug(old + " is already forging");
            return old;
        }
//        listeners.notify(generator, Generator.GeneratorEvent.START_FORGING);
        log.debug(generator + " started");
        return generator;
    }

    public GeneratorEntity stopForging(byte[] keySeed) {
        GeneratorEntity generator = generators.remove(Convert.getId(Crypto.getPublicKey(keySeed)));
        if (generator != null) {
            globalSync.updateLock();
            try {
//                sortedForgers = null;
                generateBlocksThread.resetSortedForgers();
            } finally {
                globalSync.updateUnlock();
            }
            log.debug(generator + " stopped");
//            listeners.notify(generator, Generator.GeneratorEvent.STOP_FORGING);
        }
        return generator;
    }

    public int stopForging() {
        int count = generators.size();
        Iterator<GeneratorEntity> iter = generators.values().iterator();
        while (iter.hasNext()) {
            GeneratorEntity generator = iter.next();
            iter.remove();
            log.debug(generator + " stopped");
//            listeners.notify(generator, Generator.GeneratorEvent.STOP_FORGING);
        }
        globalSync.updateLock();
        try {
//            sortedForgers = null;
            generateBlocksThread.resetSortedForgers();
        } finally {
            globalSync.updateUnlock();
        }
        return count;
    }

    public GeneratorEntity getGenerator(long id) {
        return generators.get(id);
    }

    public int getGeneratorCount() {
        return generators.size();
    }

    public Map<Long, GeneratorEntity> getGeneratorsMap() {
        return generators;
    }

    public Collection<GeneratorEntity> getAllGenerators() {
        return allGenerators;
    }

    public List<GeneratorEntity> getSortedForgers() {
//        List<GeneratorEntity> forgers = sortedForgers;
        List<GeneratorEntity> forgers = generateBlocksThread.getSortedForgers();
        return forgers == null ? Collections.emptyList() : forgers;
    }

    public long getNextHitTime(long lastBlockId, int curTime) {
        globalSync.readLock();
        try {
//            if (lastBlockId == Generator.lastBlockId && sortedForgers != null) {
            List<GeneratorEntity> sortedForgers = generateBlocksThread.getSortedForgers();
            if (sortedForgers != null) {
                for (GeneratorEntity generator : sortedForgers) {
                    if (generator.getHitTime() >= curTime - propertiesHolder.FORGING_DELAY()) {
                        return generator.getHitTime();
                    }
                }
            }
            return 0;
        } finally {
            globalSync.readUnlock();
        }
    }

    public void setDelay(int delay) {
//        Generator.delayTime = delay;
    }

//    public boolean verifyHit(BigInteger hit, BigInteger effectiveBalance, Block previousBlock, int timestamp) {
    public boolean verifyHit(GeneratorEntity generator, Block previousBlock, int timestamp) {
        int elapsedTime = timestamp - previousBlock.getTimestamp();
        if (elapsedTime <= 0) {
            return false;
        }
        BigInteger effectiveBaseTarget = BigInteger.valueOf(previousBlock.getBaseTarget())
            .multiply(generator.getEffectiveBalance());
        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(elapsedTime - 1));
        BigInteger target = prevTarget.add(effectiveBaseTarget);
        boolean ret = generator.getHit().compareTo(target) < 0
            && (generator.getHit().compareTo(prevTarget) >= 0
            || elapsedTime > 3600
            || propertiesHolder.isOffline());
        if (!ret) {
            log.warn("target: {}, hit: {}, verification failed!", target, generator.getHit());
        }
        return ret;
    }

    public void suspendForging() {
        if (!suspendForging) {
            globalSync.updateLock();
            suspendForging = true;
            globalSync.updateUnlock();
            log.info("Block generation was suspended");
        }
    }

    public void resumeForging() {
        if (suspendForging) {
            globalSync.updateLock();
            suspendForging = false;
            globalSync.updateUnlock();
            log.debug("Forging was resumed");
        }
    }

    public boolean forge(Block lastBlock, int generationLimit, GeneratorEntity generator) throws BlockchainProcessor.BlockNotAcceptedException {
        int timestamp = generator.getTimestamp(generationLimit);
        int[] timeoutAndVersion = getBlockTimeoutAndVersion(timestamp, generationLimit, lastBlock);
        if (timeoutAndVersion == null) {
            return false;
        }
        int timeout = timeoutAndVersion[0];
        if (!verifyHit(generator, lastBlock, timestamp)) {
            log.debug(this.toString() + " failed to forge at " + (timestamp + timeout) + " height " + lastBlock.getHeight() + " " +
                "last " +
                "timestamp " + lastBlock.getTimestamp());
            return false;
        }
        int start = timeService.getEpochTime();
        while (true) {
            try {
                blockchainProcessor.generateBlock(generator.getKeySeed(), timestamp + timeout, timeout, timeoutAndVersion[1]);
                setDelay(propertiesHolder.FORGING_DELAY());
                return true;
            } catch (BlockchainProcessor.TransactionNotAcceptedException e) {
                // the bad transaction has been expunged, try again
                if (timeService.getEpochTime() - start > 10) { // give up after trying for 10 s
                    throw e;
                }
            }
        }
    }

    /**
     * Return block timestamp shift
     *
     * @return 0 - when adaptive forging is disabled or forging process should be continued
     * -1 - when adaptive forging is enabled and forging process should be terminated for current attempt
     * >0 - when adaptive forging is enabled and new block should be generated with timestamp = calculated timestamp + returned value
     */
    private int[] getBlockTimeoutAndVersion(int timestamp, int generationLimit, Block lastBlock) {
        boolean isAdaptiveForging = blockchainConfig.getCurrentConfig().isAdaptiveForgingEnabled();
        int version = isAdaptiveForging ? Block.REGULAR_BLOCK_VERSION : Block.LEGACY_BLOCK_VERSION;
        int timeout = 0;
        // transactions at generator hit time
        boolean noTransactionsAtTimestamp = isAdaptiveForging && blockchainProcessor.getUnconfirmedTransactions(lastBlock, timestamp, 1).size() == 0;
        int planedBlockTime = timestamp - lastBlock.getTimestamp();
//        LOG.debug("Planed blockTime {} - uncg {}, unct {}", planedBlockTime,
//                noTransactionsAtGenerationLimit, noTransactionsAtTimestamp);
        int adaptiveBlockTime = blockchainConfig.getCurrentConfig().getAdaptiveBlockTime();
        if (isAdaptiveForging // try to calculate timeout only when adaptive forging enabled
            && noTransactionsAtTimestamp   // means that if no timeout provided, block will be empty
            && planedBlockTime < adaptiveBlockTime // calculate timeout only for faster than predefined empty block
        ) {
            int actualBlockTime = generationLimit - lastBlock.getTimestamp();
            log.trace("Act time:" + actualBlockTime);
            if (actualBlockTime >= adaptiveBlockTime) {
                // empty block can be generated by timeout
                version = Block.ADAPTIVE_BLOCK_VERSION;
            } else if (actualBlockTime >= planedBlockTime) {
                int txsAtGenerationLimit = blockchainProcessor.getUnconfirmedTransactions(lastBlock, generationLimit, 1).size();
                if (txsAtGenerationLimit == 1) {
                    // block with transactions can be generated (unc transactions exist at current time, required timeout)
                    version = Block.INSTANT_BLOCK_VERSION;
                } else {
                    return null;
                }
            } else {
                return null;
            }
            timeout = generationLimit - timestamp;
            log.trace("Timeout:" + timeout);
            return new int[]{timeout, version};
        }
        if (noTransactionsAtTimestamp) {
            version = Block.ADAPTIVE_BLOCK_VERSION;
        }
        return new int[]{timeout, version};
    }

/*
    private int getTimestamp(int generationLimit) {
        return (generationLimit - hitTime > 3600) ? generationLimit : (int) hitTime + 1;
    }
*/

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }
}
