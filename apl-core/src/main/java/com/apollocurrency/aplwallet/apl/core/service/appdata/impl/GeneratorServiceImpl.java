/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata.impl;

import com.apollocurrency.aplwallet.apl.core.app.runnable.GenerateBlocksTask;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.util.db.DbTransactionHelper;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.GeneratorMemoryEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.GeneratorService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxSerializer;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
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

@Slf4j
@Singleton
public class GeneratorServiceImpl implements GeneratorService {

    private static final String BACKGROUND_SERVICE_NAME = "GeneratorService";

    private  final ConcurrentMap<Long, GeneratorMemoryEntity> generators = new ConcurrentHashMap<>();
    private  final Collection<GeneratorMemoryEntity> allGenerators = Collections.unmodifiableCollection(generators.values());
    private final PropertiesHolder propertiesHolder;
    private final int MAX_FORGERS;
    private final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;
    private final GlobalSync globalSync;
    private BlockchainProcessor blockchainProcessor;
    private final TimeService timeService;
    private final AccountService accountService;
    private static volatile boolean suspendForging = false;

    private GenerateBlocksTask generateBlocksTask;

    @Inject
    public GeneratorServiceImpl(PropertiesHolder propertiesHolder,
                                BlockchainConfig blockchainConfig,
                                Blockchain blockchain,
                                GlobalSync globalSync,
                                TransactionProcessor transactionProcessor,
                                TimeService timeService,
                                AccountService accountService,
                                TaskDispatchManager taskDispatchManager, DatabaseManager databaseManager) {
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder);
        this.blockchainConfig = blockchainConfig;
        this.blockchain = blockchain;
        this.globalSync = globalSync;
        this.timeService = timeService;
        this.accountService = accountService;
        this.MAX_FORGERS = propertiesHolder.getIntProperty("apl.maxNumberOfForgers");

        if (!propertiesHolder.isLightClient()) {
            generateBlocksTask = new GenerateBlocksTask(this.propertiesHolder, this.globalSync,
                this.blockchain, this.blockchainConfig, this.timeService,
                transactionProcessor, this);
            taskDispatchManager.newBackgroundDispatcher(BACKGROUND_SERVICE_NAME)
                .schedule(Task.builder()
                    .name("GenerateBlocks")
                    .initialDelay(500)
                    .delay(500)
                    .task(()-> {
                        DbTransactionHelper.executeInTransaction(databaseManager.getDataSource(), ()-> {
                            generateBlocksTask.run();
                        });
                    })
                    .build());
        }
    }

    @Override
    public GeneratorMemoryEntity startForging(byte[] keySeed) {
        if (generators.size() >= MAX_FORGERS) {
            throw new RuntimeException("Cannot forge with more than " + MAX_FORGERS + " accounts on the same node");
        }
        byte[] publicKey = Crypto.getPublicKey(keySeed);
        long accountId = AccountService.getId(publicKey);
        GeneratorMemoryEntity generator = new GeneratorMemoryEntity(keySeed, publicKey, accountId);
        globalSync.updateLock();
        try {
            if (blockchain.getHeight() >= blockchainConfig.getLastKnownBlock()) {
                this.setLastBlock(blockchain.getLastBlock(), generator);
                if (generateBlocksTask != null) {
                    generateBlocksTask.resetSortedForgers();
                }
            }
        } finally {
            globalSync.updateUnlock();
        }
        GeneratorMemoryEntity old = generators.putIfAbsent(generator.getAccountId(), generator);
        if (old != null) {
            log.debug(old + " is already forging");
            return old;
        }
        log.debug(generator + " started");
        return generator;
    }

    @Override
    public GeneratorMemoryEntity stopForging(byte[] keySeed) {
        long start = System.currentTimeMillis();
        GeneratorMemoryEntity generator = generators.remove(Convert.getId(Crypto.getPublicKey(keySeed)));
        if (generator != null && generateBlocksTask != null) {
            globalSync.updateLock();
            try {
                generateBlocksTask.resetSortedForgers();
            } finally {
                globalSync.updateUnlock();
            }
            log.debug(generator + " stopped ({} ms)", (System.currentTimeMillis() - start));
        }
        return generator;
    }

    @Override
    public int stopForging() {
        long start = System.currentTimeMillis();
        int count = generators.size();
        Iterator<GeneratorMemoryEntity> iter = generators.values().iterator();
        while (iter.hasNext()) {
            GeneratorMemoryEntity generator = iter.next();
            iter.remove();
            log.debug(generator + " stopped");
        }
        globalSync.updateLock();
        try {
            if (generateBlocksTask != null) {
                generateBlocksTask.resetSortedForgers();
            }
        } finally {
            globalSync.updateUnlock();
        }
        log.debug("stopped forging ({} ms)", (System.currentTimeMillis() - start));
        return count;
    }

    @Override
    public GeneratorMemoryEntity getGenerator(long id) {
        return generators.get(id);
    }

    @Override
    public int getGeneratorCount() {
        return generators.size();
    }

    @Override
    public Map<Long, GeneratorMemoryEntity> getGeneratorsMap() {
        return generators;
    }

    @Override
    public Collection<GeneratorMemoryEntity> getAllGenerators() {
        return allGenerators;
    }

    @Override
    public List<GeneratorMemoryEntity> getSortedForgers() {
        List<GeneratorMemoryEntity> forgers = generateBlocksTask != null ? generateBlocksTask.getSortedForgers() : null;
        return forgers == null ? Collections.emptyList() : forgers;
    }

    @Override
    public long getNextHitTime(long lastBlockId, int curTime) {
        globalSync.readLock();
        try {
            List<GeneratorMemoryEntity> sortedForgers = generateBlocksTask != null ? generateBlocksTask.getSortedForgers() : null;
            if (sortedForgers != null) {
                for (GeneratorMemoryEntity generator : sortedForgers) {
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

    @Override
    public void setDelay(int delay) {
        generateBlocksTask.setDelayTime(delay);
    }

    public boolean verifyHit(BigInteger hit, BigInteger effectiveBalance, Block previousBlock, int timestamp) {
        int elapsedTime = timestamp - previousBlock.getTimestamp();
        if (elapsedTime <= 0) {
            return false;
        }
        BigInteger effectiveBaseTarget = BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(effectiveBalance);
        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(elapsedTime - 1));
        BigInteger target = prevTarget.add(effectiveBaseTarget);
        boolean ret = hit.compareTo(target) < 0
            && (hit.compareTo(prevTarget) >= 0
            || elapsedTime > 3600
            || propertiesHolder.isOffline());
        if (!ret) {
            log.warn("target: {}, hit: {}, verification failed!", target, hit);
        }
        return ret;
    }

    @Override
    public boolean verifyHit(GeneratorMemoryEntity generator, Block previousBlock, int timestamp) {
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

    @Override
    public void setLastBlock(Block lastBlock, GeneratorMemoryEntity generator) {
        int height = lastBlock.getHeight();
        Account account = accountService.getAccount(generator.getAccountId(), height);
        if (account == null) {
            generator.setEffectiveBalance(BigInteger.ZERO);
        } else {
            generator.setEffectiveBalance(
                BigInteger.valueOf(Math.max(
                    accountService.getEffectiveBalanceAPL(account, height, true), 0)));
        }
        if (generator.getEffectiveBalance().signum() == 0) {
            generator.setHitTime(0);
            generator.setHit(BigInteger.ZERO);
            return;
        }
        generator.setHit( getHit(generator.getPublicKey(), lastBlock));
        generator.setHitTime( getHitTime(generator.getEffectiveBalance(), generator.getHit(), lastBlock));
        generator.setDeadline( Math.max(generator.getHitTime() - lastBlock.getTimestamp(), 0));
    }

    @Override
    public BigInteger getHit(byte[] publicKey, Block block) {
        MessageDigest digest = Crypto.sha256();
        digest.update(block.getGenerationSignature());
        byte[] generationSignatureHash = digest.digest(publicKey);
        return new BigInteger(1,
            new byte[]{generationSignatureHash[7], generationSignatureHash[6],
                generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3],
                generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
    }

    @Override
    public long getHitTime(BigInteger effectiveBalance, BigInteger hit, Block block) {
        return block.getTimestamp()
            + hit.divide(BigInteger.valueOf(block.getBaseTarget()).multiply(effectiveBalance)).longValue();
    }

    @Override
    public void suspendForging() {
        if (!suspendForging) {
            globalSync.updateLock();
            suspendForging = true;
            if (generateBlocksTask != null) {
                generateBlocksTask.setSuspendForging(suspendForging);
            }
            globalSync.updateUnlock();
            log.info("Block generation was suspended = {}", suspendForging);
        }
    }

    @Override
    public void resumeForging() {
        if (suspendForging) {
            globalSync.updateLock();
            suspendForging = false;
            if (generateBlocksTask != null) {
                generateBlocksTask.setSuspendForging(suspendForging);
            }
            globalSync.updateUnlock();
            log.debug("Forging was resumed = {}", !suspendForging);
        }
    }

    @Override
    public boolean forge(Block lastBlock, int generationLimit, GeneratorMemoryEntity generator)
        throws BlockchainProcessor.BlockNotAcceptedException {
        long startLog = timeService.systemTimeMillis();
        int timestamp = generator.getTimestamp(generationLimit);
        if (!verifyHit(generator, lastBlock, timestamp)) {
            log.debug("{} failed to forge at {} height {} last timestamp {}", generator, timestamp, lastBlock.getHeight(), lastBlock.getTimestamp());
            return false;
        }
        while (true) {
            try {
                int[] timeoutAndVersion = getBlockTimeoutAndVersion(timestamp, generationLimit, lastBlock);
                if (timeoutAndVersion == null) {
                    log.trace("{} skip turn to generate block", generator);
                    return false;
                }
                int timeout = timeoutAndVersion[0];
                int blockVersion = timeoutAndVersion[1];
                lookupBlockchainProcessor().generateBlock(generator.getKeySeed(), timestamp + timeout, timeout, blockVersion);
                setDelay(propertiesHolder.FORGING_DELAY());
                log.debug("{} stopped forge loop in ({} ms)", generator, (System.currentTimeMillis() - startLog));
                return true;
            } catch (BlockchainProcessor.MempoolStateDesyncException e) {
                log.debug("Mempool desync {}", e.getMessage()); // try another time
            } catch (BlockchainProcessor.TransactionNotAcceptedException e) {
                String txJson = txJson(e.getTransaction());
                log.debug("Transaction not accepted during block generation {} , cause {}", txJson, e.getMessage());
                if (timeService.systemTimeMillis() - startLog > 20_000) {
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
        lookupBlockchainProcessor();
        boolean noTransactionsAtTimestamp = isAdaptiveForging
            && blockchainProcessor.getUnconfirmedTransactions(lastBlock, timestamp, 1).size() == 0;
        int planedBlockTime = timestamp - lastBlock.getTimestamp();
//        LOG.debug("Planed blockTime {} - uncg {}, unct {}", planedBlockTime,
//                noTransactionsAtGenerationLimit, noTransactionsAtTimestamp);
        int adaptiveBlockTime = blockchainConfig.getCurrentConfig().getAdaptiveBlockTime();
        if (// try to calculate timeout only when adaptive forging enabled
            noTransactionsAtTimestamp   // means that if no timeout provided, block will be empty
            && planedBlockTime < adaptiveBlockTime // calculate timeout only for faster than predefined empty block
        ) {
            int actualBlockTime = generationLimit - lastBlock.getTimestamp();
            int txsAtGenerationLimit = blockchainProcessor.getUnconfirmedTransactions(lastBlock, generationLimit, 1).size();
            if (actualBlockTime >= adaptiveBlockTime && txsAtGenerationLimit == 0) {
                // empty block can be generated by timeout
                version = Block.ADAPTIVE_BLOCK_VERSION;
            } else if (actualBlockTime >= planedBlockTime && txsAtGenerationLimit == 1) {
                // block with transactions can be generated (unc transactions exist at current time, required timeout)
                version = Block.INSTANT_BLOCK_VERSION;
            } else {
                log.trace("Skip generation iteration: tx at generation limit {}, timestamp {}, generation limit {}, actual block time {}, planed block time {}", txsAtGenerationLimit, timestamp, generationLimit, actualBlockTime, planedBlockTime);
                return null;
            }
            timeout = generationLimit - timestamp;
            log.trace("Set block timeout: {}, version {}, timestamp {}, generation limit {}, actual block time {}, planed block time {}", timeout, version, timestamp, generationLimit, actualBlockTime, planedBlockTime);
            return new int[]{timeout, version};
        }
        if (noTransactionsAtTimestamp) {
            version = Block.ADAPTIVE_BLOCK_VERSION;
        }
        log.trace("Set forging params: timeout {}, version {}, timestamp {}, generation limit {}, planed block time {}", timeout, version, timestamp, generationLimit, planedBlockTime);
        return new int[]{timeout, version};
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }

    private String txJson(Transaction tx) {
        TxSerializer serializer = TxBContext.newInstance(blockchainConfig.getChain()).createSerializer(tx.getVersion());
        PayloadResult jsonBuffer = PayloadResult.createJsonResult();
        serializer.serialize(tx, jsonBuffer);
        return new String(jsonBuffer.array());
    }
}
