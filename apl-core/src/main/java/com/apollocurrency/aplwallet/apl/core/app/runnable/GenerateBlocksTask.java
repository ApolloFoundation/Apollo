/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.GeneratorMemoryEntity;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.GeneratorServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class GenerateBlocksTask implements Runnable {

    private volatile boolean logged;
    private long lastBlockId;
    private volatile boolean suspendForging = false;

    private final PropertiesHolder propertiesHolder;
    private final GlobalSync globalSync;
    private final Blockchain blockchain;
    private final BlockchainConfig blockchainConfig;
    private volatile TimeService timeService;
    private final TransactionProcessor transactionProcessor;
    private BlockchainProcessor blockchainProcessor;
    private final GeneratorServiceImpl generatorService;

    private volatile List<GeneratorMemoryEntity> sortedForgers = null;
    private int delayTime;
    private volatile boolean conditionToResetForgers = false;

    public GenerateBlocksTask(PropertiesHolder propertiesHolder,
                              GlobalSync globalSync,
                              Blockchain blockchain,
                              BlockchainConfig blockchainConfig,
                              TimeService timeService,
                              TransactionProcessor transactionProcessor,
                              GeneratorServiceImpl generatorService) {
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder);
        this.globalSync = Objects.requireNonNull(globalSync);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.timeService = Objects.requireNonNull(timeService);
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.delayTime = this.propertiesHolder.FORGING_DELAY();
        this.generatorService = Objects.requireNonNull(generatorService);
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        log.trace("run - generateBlocksThread ({})", start);
        if (suspendForging) {
            log.trace("run - suspendForging = {}", suspendForging);
            return;
        }
        if (generatorService.getGeneratorsMap().isEmpty()) {
            resetSortedForgers();
            return;
        }
        try {
            try {
                globalSync.updateLock();
                long forgingIterationStart = System.currentTimeMillis();
                if (checkIfResetNeeded()) {
                    resetSortedForgers();
                }
                log.trace("Acquire generation lock");
                try {
                    Block lastBlock = blockchain.getLastBlock();
                    if (lastBlock == null || lastBlock.getHeight() < blockchainConfig.getLastKnownBlock()) {
                        log.trace("run - lastBlock = {}", lastBlock);
                        return;
                    }
                    final int generationLimit = timeService.getEpochTime() - delayTime;
                    if (lastBlock.getId() != lastBlockId || sortedForgers == null) {
                        lastBlockId = lastBlock.getId();
                        log.trace("run - lastBlockId = {} ({} ms)", lastBlockId, (System.currentTimeMillis() - start));
                        Map<Long, GeneratorMemoryEntity> generatorsMap = generatorService.getGeneratorsMap();
                        if (lastBlock.getTimestamp() > timeService.getEpochTime() - 600) {
                            log.trace("run - getTimestamp = {} > {}", lastBlock.getTimestamp(), timeService.getEpochTime() - 600);
                            Block previousBlock = blockchain.getBlock(lastBlock.getPreviousBlockId());
                            for (GeneratorMemoryEntity generator : generatorsMap.values()) {
                                log.trace("run - generator.setLastBlock() 1. = {}", generator);
                                generatorService.setLastBlock(previousBlock, generator);
                                int timestamp = generator.getTimestamp(generationLimit);
                                if (timestamp != generationLimit && generator.getHitTime() > 0 && timestamp < lastBlock.getTimestamp() - lastBlock.getTimeout()) {
                                    log.debug("Pop off: {} will pop off last block {}", generator.toString(), lastBlock.getStringId());
                                    List<Block> poppedOffBlock = lookupBlockchainProcessor().popOffToCommonBlock(previousBlock);
                                    for (Block block : poppedOffBlock) {
                                        transactionProcessor.processLater(block.getTransactions());
                                    }
                                    lastBlock = previousBlock;
                                    lastBlockId = previousBlock.getId();
                                    break;
                                }
                            }
                        }
                        List<GeneratorMemoryEntity> forgers = new ArrayList<>();
                        for (GeneratorMemoryEntity generator : generatorsMap.values()) {
                            log.trace("run - generator.setLastBlock() 2. = {}", generator);
                            generatorService.setLastBlock(lastBlock, generator);
                            if (generator.getEffectiveBalance().signum() > 0) {
                                boolean result = forgers.add(generator);
                                log.trace("run - added ! generator = {}", result);
                            }
                        }
                        Collections.sort(forgers);
                        sortedForgers = Collections.unmodifiableList(forgers);
                        logged = false;
                        log.trace("run - set logged = {} ({} ms)", logged, (System.currentTimeMillis() - start));
                    }
                    if (!logged) {
                        for (GeneratorMemoryEntity generator : sortedForgers) {
                            if (generator.getHitTime() - generationLimit > 60) {
                                break;
                            }
                            log.debug(generator.toString());
                            logged = true;
                            log.trace("run - unset logged = {}", logged);
                        }
                    }
                    for (GeneratorMemoryEntity generator : sortedForgers) {
                        if (suspendForging) {
                            break;
                        }
                        boolean fastEnough = generator.getHitTime() <= generationLimit;
                        if (!fastEnough) {
                            log.trace("Skip {}, Reason: Too slow. Generation limit {} ", generator, generationLimit);
                            continue;
                        }
                        boolean forged = generatorService.forge(lastBlock, generationLimit, generator);
                        if (!forged) {
                            log.trace("{} hasn't generated a block. Go to next", generator);
                        } else {
                            break;
                        }
                    }
                } finally {
                    globalSync.updateUnlock();
                    log.trace("Forging job is done in ({} ms), forging time ({} ms)", (System.currentTimeMillis() - start), (System.currentTimeMillis() - forgingIterationStart));
                }
            } catch (Exception e) {
                log.error("Error in block generation thread ({} ms)", (System.currentTimeMillis() - start), e);
                log.trace("Stack trace = {}", ThreadUtils.getStackTrace(e));
            }
        } catch (Throwable t) {
            log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }
    }

    public synchronized boolean resetForgersAsync() {
        var rc = conditionToResetForgers;
        conditionToResetForgers = true;
        return rc;
    }

    public synchronized boolean checkIfResetNeeded() {
        return conditionToResetForgers;
    }

    public List<GeneratorMemoryEntity> getSortedForgers() {
        return sortedForgers;
    }

    public synchronized void resetSortedForgers() {
        sortedForgers = null;
        conditionToResetForgers = false;
    }

    public void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }

    public void setSuspendForging(boolean suspendForging) {
        this.suspendForging = suspendForging;
    }
}
