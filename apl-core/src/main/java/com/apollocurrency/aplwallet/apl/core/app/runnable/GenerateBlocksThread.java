/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.apollocurrency.aplwallet.apl.core.app.Generator;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateBlocksThread implements Runnable {

    private volatile boolean logged;
    private long lastBlockId;
    private static volatile boolean suspendForging = false;
    private static final ConcurrentMap<Long, Generator> generators = new ConcurrentHashMap<>();

    private final PropertiesHolder propertiesHolder;
    private final GlobalSync globalSync;
    private final Blockchain blockchain;
    private final BlockchainConfig blockchainConfig;
    private volatile TimeService timeService;
    private final TransactionProcessor transactionProcessor;
    private BlockchainProcessor blockchainProcessor;

    private static volatile List<Generator> sortedForgers = null;
    private int delayTime;

    public GenerateBlocksThread(PropertiesHolder propertiesHolder,
                                GlobalSync globalSync,
                                Blockchain blockchain,
                                BlockchainConfig blockchainConfig,
                                TimeService timeService,
                                TransactionProcessor transactionProcessor) {
        this.propertiesHolder = propertiesHolder;
        this.globalSync = globalSync;
        this.blockchain = blockchain;
        this.blockchainConfig = blockchainConfig;
        this.timeService = timeService;
        this.transactionProcessor = transactionProcessor;
        this.delayTime = this.propertiesHolder.FORGING_DELAY();
    }

    @Override
    public void run() {
        if (suspendForging) {
            return;
        }
        try {
            try {
                globalSync.updateLock();
                try {
                    Block lastBlock = blockchain.getLastBlock();
                    if (lastBlock == null || lastBlock.getHeight() < blockchainConfig.getLastKnownBlock()) {
                        return;
                    }
                    final int generationLimit = timeService.getEpochTime() - delayTime;
                    if (lastBlock.getId() != lastBlockId || sortedForgers == null) {
                        lastBlockId = lastBlock.getId();
                        if (lastBlock.getTimestamp() > timeService.getEpochTime() - 600) {
                            Block previousBlock = blockchain.getBlock(lastBlock.getPreviousBlockId());
                            for (Generator generator : generators.values()) {
/*
                                generator.setLastBlock(previousBlock);
                                int timestamp = generator.getTimestamp(generationLimit);
                                if (timestamp != generationLimit && generator.getHitTime() > 0 && timestamp < lastBlock.getTimestamp() - lastBlock.getTimeout()) {
                                    log.debug("Pop off: " + generator.toString() + " will pop off last block " + lastBlock.getStringId());
                                    List<Block> poppedOffBlock = blockchainProcessor.popOffToCommonBlock(previousBlock);
                                    for (Block block : poppedOffBlock) {
                                        transactionProcessor.processLater(block.getOrLoadTransactions());
                                    }
                                    lastBlock = previousBlock;
                                    lastBlockId = previousBlock.getId();
                                    break;
                                }
*/
                            }
                        }
                        List<Generator> forgers = new ArrayList<>();
                        for (Generator generator : generators.values()) {
/*
                            generator.setLastBlock(lastBlock);
                            if (generator.effectiveBalance.signum() > 0) {
                                forgers.add(generator);
                            }
*/
                        }
                        Collections.sort(forgers);
                        sortedForgers = Collections.unmodifiableList(forgers);
                        logged = false;
                    }
                    if (!logged) {
                        for (Generator generator : sortedForgers) {
                            if (generator.getHitTime() - generationLimit > 60) {
                                break;
                            }
                            log.debug(generator.toString());
                            logged = true;
                        }
                    }
                    for (Generator generator : sortedForgers) {
                        if (suspendForging) {
                            break;
                        }
                        if (generator.getHitTime() > generationLimit || generator.forge(lastBlock, generationLimit)) {
                            return;
                        }
                    }
                } finally {
                    globalSync.updateUnlock();
                }
            } catch (Exception e) {
                log.info("Error in block generation thread", e);
            }
        } catch (Throwable t) {
            log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }

    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }

}
