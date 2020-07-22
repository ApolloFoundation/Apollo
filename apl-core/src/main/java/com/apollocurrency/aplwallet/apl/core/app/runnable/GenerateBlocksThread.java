/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import javax.enterprise.inject.spi.CDI;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.GeneratorEntity;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.GeneratorServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateBlocksThread implements Runnable {

    private volatile boolean logged;
    private long lastBlockId;
    private static volatile boolean suspendForging = false;

    private final PropertiesHolder propertiesHolder;
    private final GlobalSync globalSync;
    private final Blockchain blockchain;
    private final BlockchainConfig blockchainConfig;
    private volatile TimeService timeService;
    private final TransactionProcessor transactionProcessor;
    private BlockchainProcessor blockchainProcessor;
    private AccountService accountService;
    private final GeneratorServiceImpl generatorService;

    private static volatile List<GeneratorEntity> sortedForgers = null;
    private int delayTime;

    public GenerateBlocksThread(PropertiesHolder propertiesHolder,
                                GlobalSync globalSync,
                                Blockchain blockchain,
                                BlockchainConfig blockchainConfig,
                                TimeService timeService,
                                TransactionProcessor transactionProcessor,
                                AccountService accountService,
                                GeneratorServiceImpl generatorService) {
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder);
        this.globalSync = Objects.requireNonNull(globalSync);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.timeService = Objects.requireNonNull(timeService);
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.delayTime = this.propertiesHolder.FORGING_DELAY();
        this.accountService = Objects.requireNonNull(accountService);
        this.generatorService = Objects.requireNonNull(generatorService);
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

                        Map<Long, GeneratorEntity> generatorsMap = generatorService.getGeneratorsMap();
                        if (lastBlock.getTimestamp() > timeService.getEpochTime() - 600) {
                            Block previousBlock = blockchain.getBlock(lastBlock.getPreviousBlockId());
                            for (GeneratorEntity generator : generatorsMap.values()) {
//                            for (Generator generator : generators.values()) {
//                                generator.setLastBlock(previousBlock);
                                generatorService.setLastBlock(previousBlock, generator);
                                int timestamp = generator.getTimestamp(generationLimit);
                                if (timestamp != generationLimit && generator.getHitTime() > 0 && timestamp < lastBlock.getTimestamp() - lastBlock.getTimeout()) {
                                    log.debug("Pop off: {} will pop off last block {}", generator.toString(), lastBlock.getStringId());
                                    List<Block> poppedOffBlock = lookupBlockchainProcessor().popOffToCommonBlock(previousBlock);
                                    for (Block block : poppedOffBlock) {
                                        transactionProcessor.processLater(block.getOrLoadTransactions());
                                    }
                                    lastBlock = previousBlock;
                                    lastBlockId = previousBlock.getId();
                                    break;
                                }
                            }
                        }
                        List<GeneratorEntity> forgers = new ArrayList<>();
                        for (GeneratorEntity generator : generatorsMap.values()) {
                            generatorService.setLastBlock(lastBlock, generator);
                            if (generator.getEffectiveBalance().signum() > 0) {
                                forgers.add(generator);
                            }
                        }
                        Collections.sort(forgers);
                        sortedForgers = Collections.unmodifiableList(forgers);
                        logged = false;
                    }
                    if (!logged) {
                        for (GeneratorEntity generator : sortedForgers) {
                            if (generator.getHitTime() - generationLimit > 60) {
                                break;
                            }
                            log.debug(generator.toString());
                            logged = true;
                        }
                    }
                    for (GeneratorEntity generator : sortedForgers) {
                        if (suspendForging) {
                            break;
                        }
                        if (generator.getHitTime() > generationLimit
                            || generatorService.forge(lastBlock, generationLimit, generator)) {
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

/*
    public void setLastBlock(Block lastBlock, GeneratorEntity generator) {
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

    public BigInteger getHit(byte[] publicKey, Block block) {
        MessageDigest digest = Crypto.sha256();
        digest.update(block.getGenerationSignature());
        byte[] generationSignatureHash = digest.digest(publicKey);
        return new BigInteger(1,
            new byte[]{generationSignatureHash[7], generationSignatureHash[6],
                generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3],
                generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
    }

    public long getHitTime(BigInteger effectiveBalance, BigInteger hit, Block block) {
        return block.getTimestamp()
            + hit.divide(BigInteger.valueOf(block.getBaseTarget()).multiply(effectiveBalance)).longValue();
    }
*/

    public List<GeneratorEntity> getSortedForgers() {
        return sortedForgers;
    }

    public void resetSortedForgers() {
        sortedForgers = null;
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }

}
