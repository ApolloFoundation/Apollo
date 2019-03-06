/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import com.apollocurrency.aplwallet.apl.util.ThreadPool;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.enterprise.inject.spi.CDI;

public final class Generator implements Comparable<Generator> {
    private static final Logger LOG = getLogger(Generator.class);


    public enum Event {
        GENERATION_DEADLINE, START_FORGING, STOP_FORGING
    }

    // TODO: YL remove static instance later

    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    private static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    private static GlobalSync globalSync = CDI.current().select(GlobalSync.class).get();
    private static BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
    private static TransactionProcessor transactionProcessor = CDI.current().select(TransactionProcessorImpl.class).get();
    private static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();

    private static final int MAX_FORGERS = propertiesHolder.getIntProperty("apl.maxNumberOfForgers");
    private static final byte[] fakeForgingPublicKey = propertiesHolder.getBooleanProperty("apl.enableFakeForging") ?
            Account.getPublicKey(Convert.parseAccountId(propertiesHolder.getStringProperty("apl.fakeForgingAccount"))) : null;
    private static volatile boolean suspendForging = false;
    private static final Listeners<Generator,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<Long, Generator> generators = new ConcurrentHashMap<>();
    private static final Collection<Generator> allGenerators = Collections.unmodifiableCollection(generators.values());
    private static volatile List<Generator> sortedForgers = null;
    private static long lastBlockId;
    private static int delayTime = propertiesHolder.FORGING_DELAY();

    private static final Runnable generateBlocksThread = new Runnable() {

        private volatile boolean logged;

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
                                    generator.setLastBlock(previousBlock);
                                    int timestamp = generator.getTimestamp(generationLimit);
                                    if (timestamp != generationLimit && generator.getHitTime() > 0 && timestamp < lastBlock.getTimestamp() - lastBlock.getTimeout()) {
                                        LOG.debug("Pop off: " + generator.toString() + " will pop off last block " + lastBlock.getStringId());
                                        List<Block> poppedOffBlock = blockchainProcessor.popOffTo(previousBlock);
                                        for (Block block : poppedOffBlock) {
                                            transactionProcessor.processLater(block.getTransactions());
                                        }
                                        lastBlock = previousBlock;
                                        lastBlockId = previousBlock.getId();
                                        break;
                                    }
                                }
                            }
                            List<Generator> forgers = new ArrayList<>();
                            for (Generator generator : generators.values()) {
                                generator.setLastBlock(lastBlock);
                                if (generator.effectiveBalance.signum() > 0) {
                                    forgers.add(generator);
                                }
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
                                LOG.debug(generator.toString());
                                logged = true;
                            }
                        }
                        for (Generator generator : sortedForgers) {
                            if (generator.getHitTime() > generationLimit || generator.forge(lastBlock, generationLimit)) {
                                return;
                            }
                        }
                    } finally {
                        globalSync.updateUnlock();
                    }
                } catch (Exception e) {
                    LOG.info("Error in block generation thread", e);
                }
            } catch (Throwable t) {
                LOG.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    static void init() {
        if (!propertiesHolder.isLightClient()) {
            ThreadPool.scheduleThread("GenerateBlocks", generateBlocksThread, 500, TimeUnit.MILLISECONDS);
        }        
    }

    public static boolean addListener(Listener<Generator> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Generator> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static Generator startForging(byte[] keySeed) {
        if (generators.size() >= MAX_FORGERS) {
            throw new RuntimeException("Cannot forge with more than " + MAX_FORGERS + " accounts on the same node");
        }
        Generator generator = new Generator(keySeed);
        Generator old = generators.putIfAbsent(generator.getAccountId(), generator);
        if (old != null) {
            LOG.debug(old + " is already forging");
            return old;
        }
        listeners.notify(generator, Event.START_FORGING);
        LOG.debug(generator + " started");
        return generator;
    }

    public static Generator stopForging(byte[] keySeed) {
        Generator generator = generators.remove(Convert.getId(Crypto.getPublicKey(keySeed)));
        if (generator != null) {
            globalSync.updateLock();
            try {
                sortedForgers = null;
            } finally {
                globalSync.updateUnlock();
            }
            LOG.debug(generator + " stopped");
            listeners.notify(generator, Event.STOP_FORGING);
        }
        return generator;
    }

    public static int stopForging() {
        int count = generators.size();
        Iterator<Generator> iter = generators.values().iterator();
        while (iter.hasNext()) {
            Generator generator = iter.next();
            iter.remove();
            LOG.debug(generator + " stopped");
            listeners.notify(generator, Event.STOP_FORGING);
        }
        globalSync.updateLock();
        try {
            sortedForgers = null;
        } finally {
            globalSync.updateUnlock();
        }
        return count;
    }

    public static Generator getGenerator(long id) {
        return generators.get(id);
    }

    public static int getGeneratorCount() {
        return generators.size();
    }

    public static Collection<Generator> getAllGenerators() {
        return allGenerators;
    }

    public static List<Generator> getSortedForgers() {
        List<Generator> forgers = sortedForgers;
        return forgers == null ? Collections.emptyList() : forgers;
    }

    public static long getNextHitTime(long lastBlockId, int curTime) {
        globalSync.readLock();
        try {
            if (lastBlockId == Generator.lastBlockId && sortedForgers != null) {
                for (Generator generator : sortedForgers) {
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

    static void setDelay(int delay) {
        Generator.delayTime = delay;
    }

    static boolean verifyHit(BigInteger hit, BigInteger effectiveBalance, Block previousBlock, int timestamp) {
        int elapsedTime = timestamp - previousBlock.getTimestamp();
        if (elapsedTime <= 0) {
            return false;
        }
        BigInteger effectiveBaseTarget = BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(effectiveBalance);
        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(elapsedTime - 1));
        BigInteger target = prevTarget.add(effectiveBaseTarget);
        return hit.compareTo(target) < 0
                && (hit.compareTo(prevTarget) >= 0
                || elapsedTime > 3600
                || propertiesHolder.isOffline());
    }

    static BigInteger getHit(byte[] publicKey, Block block) {
        MessageDigest digest = Crypto.sha256();
        digest.update(block.getGenerationSignature());
        byte[] generationSignatureHash = digest.digest(publicKey);
        return new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
    }

    static long getHitTime(BigInteger effectiveBalance, BigInteger hit, Block block) {
        return block.getTimestamp()
                + hit.divide(BigInteger.valueOf(block.getBaseTarget()).multiply(effectiveBalance)).longValue();
    }


    private final long accountId;
    private final byte[] keySeed;
    private final byte[] publicKey;
    private volatile long hitTime;
    private volatile BigInteger hit;
    private volatile BigInteger effectiveBalance;
    private volatile long deadline;

    public Generator(long accountId, byte[] keySeed, byte[] publicKey) {
        this.accountId = accountId;
        this.keySeed = keySeed;
        this.publicKey = publicKey;
    }

    private Generator(byte[] keySeed) {
        this.keySeed = keySeed;
        this.publicKey = Crypto.getPublicKey(keySeed);
        this.accountId = Account.getId(publicKey);
        globalSync.updateLock();
        try {
            if (blockchain.getHeight() >= blockchainConfig.getLastKnownBlock()) {
                setLastBlock(blockchain.getLastBlock());
            }
            sortedForgers = null;
        } finally {
            globalSync.updateUnlock();
        }
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getDeadline() {
        return deadline;
    }

    public long getHitTime() {
        return hitTime;
    }

    @Override
    public int compareTo(Generator g) {
        int i = this.hit.multiply(g.effectiveBalance).compareTo(g.hit.multiply(this.effectiveBalance));
        if (i != 0) {
            return i;
        }
        return Long.compare(accountId, g.accountId);
    }

    @Override
    public String toString() {
        return "Forger " + Long.toUnsignedString(accountId) + " deadline " + getDeadline() + " hit " + hitTime;
    }

    private void setLastBlock(Block lastBlock) {
        int height = lastBlock.getHeight();
        Account account = Account.getAccount(accountId, height);
        if (account == null) {
            effectiveBalance = BigInteger.ZERO;
        } else {
            effectiveBalance = BigInteger.valueOf(Math.max(account.getEffectiveBalanceAPL(height), 0));
        }
        if (effectiveBalance.signum() == 0) {
            hitTime = 0;
            hit = BigInteger.ZERO;
            return;
        }
        hit = getHit(publicKey, lastBlock);
        hitTime = getHitTime(effectiveBalance, hit, lastBlock);
        deadline = Math.max(hitTime - lastBlock.getTimestamp(), 0);
        listeners.notify(this, Event.GENERATION_DEADLINE);
    }

    boolean forge(Block lastBlock, int generationLimit) throws BlockchainProcessor.BlockNotAcceptedException {
        int timestamp = getTimestamp(generationLimit);
        int[] timeoutAndVersion = getBlockTimeoutAndVersion(timestamp, generationLimit, lastBlock);
        if (timeoutAndVersion == null) {
            return false;
        }
        int timeout = timeoutAndVersion[0];
        if (!verifyHit(hit, effectiveBalance, lastBlock, timestamp)) {
            LOG.debug(this.toString() + " failed to forge at " + (timestamp + timeout) + " height " + lastBlock.getHeight() + " " +
                    "last " +
                    "timestamp " + lastBlock.getTimestamp());
            return false;
        }
        int start = timeService.getEpochTime();
        while (true) {
            try {
                blockchainProcessor.generateBlock(keySeed, timestamp  + timeout, timeout, timeoutAndVersion[1]);
                setDelay(propertiesHolder.FORGING_DELAY());
                return true;
            }
            catch (BlockchainProcessor.TransactionNotAcceptedException e) {
                // the bad transaction has been expunged, try again
                if (timeService.getEpochTime() - start > 10) { // give up after trying for 10 s
                    throw e;
                }
            }
        }
    }

    /**
     * Return block timestamp shift
     * @return 0 - when adaptive forging is disabled or forging process should be continued
     *         -1 - when adaptive forging is enabled and forging process should be terminated for current attempt
     *         >0 - when adaptive forging is enabled and new block should be generated with timestamp = calculated timestamp + returned value
     */
    private int[] getBlockTimeoutAndVersion(int timestamp, int generationLimit, Block lastBlock) {
        int version = blockchainConfig.getCurrentConfig().isAdaptiveForgingEnabled() ? Block.REGULAR_BLOCK_VERSION : Block.LEGACY_BLOCK_VERSION;
        int timeout = 0;
        // transactions at generator hit time
        boolean noTransactionsAtTimestamp =
                blockchainProcessor.getUnconfirmedTransactions(lastBlock, timestamp).size() == 0;
        // transactions at current time
        boolean noTransactionsAtGenerationLimit =
                blockchainProcessor.getUnconfirmedTransactions(lastBlock, generationLimit).size() == 0;
        int planedBlockTime = timestamp - lastBlock.getTimestamp();
        LOG.debug("Planed blockTime {} - uncg {}, unct {}", planedBlockTime,
                noTransactionsAtGenerationLimit, noTransactionsAtTimestamp);
        if (blockchainConfig.getCurrentConfig().isAdaptiveForgingEnabled() // try to calculate timeout only when adaptive forging enabled
                && noTransactionsAtTimestamp   // means that if no timeout provided, block will be empty
                && planedBlockTime < blockchainConfig.getCurrentConfig().getAdaptiveBlockTime() // calculate timeout only for faster than predefined empty block
        ) {
            int actualBlockTime = generationLimit - lastBlock.getTimestamp();
            LOG.debug("Act time:" + actualBlockTime);
            if (actualBlockTime >= blockchainConfig.getCurrentConfig().getAdaptiveBlockTime()) {
                // empty block can be generated by timeout
                version = Block.ADAPTIVE_BLOCK_VERSION;
            } else if (!noTransactionsAtGenerationLimit && actualBlockTime >= planedBlockTime) {
                // block with transactions can be generated (unc transactions exist at current time, required timeout)
                version = Block.INSTANT_BLOCK_VERSION;
            } else {
                return null;
            }
            timeout = generationLimit - timestamp;
            LOG.debug("Timeout:" + timeout);
            return new int[] {timeout, version};
        }
        if (blockchainConfig.getCurrentConfig().isAdaptiveForgingEnabled() && noTransactionsAtTimestamp) {
            version = Block.ADAPTIVE_BLOCK_VERSION;
        }
        return new int[] {timeout, version};
    }

    private int getTimestamp(int generationLimit) {
        return (generationLimit - hitTime > 3600) ? generationLimit : (int)hitTime + 1;
    }

    public static void suspendForging() {
        suspendForging = true;
        LOG.info("Block generation was suspended");
    }
    public static void resumeForging() {
        suspendForging = false;
        LOG.debug("Forging was resumed");
    }

}
