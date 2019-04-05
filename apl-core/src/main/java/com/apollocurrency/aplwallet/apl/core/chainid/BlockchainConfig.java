/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import javax.inject.Singleton;

/**
 * <p>This class used as configuration of current working chain. Commonly it mapped to an active chain described in conf/chains.json</p>
 */

@Singleton
public class BlockchainConfig {
    private static final Logger LOG = getLogger(BlockchainConfig.class);
    private int leasingDelay;
    private int minPrunableLifetime;
    private boolean enablePruning;
    private int maxPrunableLifetime;
    // lastKnownBlock must also be set in html/www/js/ars.constants.js
    private short shufflingProcessingDeadline;
    private long lastKnownBlock;
    private long unconfirmedPoolDepositAtm;
    private long shufflingDepositAtm;
    private int guaranteedBalanceConfirmations;
    private volatile HeightConfig currentConfig;
    private Chain chain;

    public BlockchainConfig() {}

    public void updateChain(Chain chain, int maxPrunableLifetime) {

        Objects.requireNonNull(chain, "Chain cannot be null");
        setFields(chain, maxPrunableLifetime);
        Map<Integer, BlockchainProperties> blockchainProperties = chain.getBlockchainProperties();
        if (blockchainProperties.isEmpty() || blockchainProperties.get(0) == null) {
            throw new IllegalArgumentException("Chain has no initial blockchain properties at height 0! ChainId = " + chain.getChainId());
        }
        currentConfig = new HeightConfig(blockchainProperties.get(0));
        LOG.debug("Switch to chain {} - {}. ChainId - {}", chain.getName(), chain.getDescription(), chain.getChainId());
    }

    private void setFields(Chain chain, int maxPrunableLifetime) {
        this.chain = chain;
        // These fields could be static constants but some fields should be scaled by blockTime
        // Block time scaling should be implemented in future
        this.leasingDelay = 1440;
        this.minPrunableLifetime = 14 * 1440 * 60; // two weeks in seconds
        this.shufflingProcessingDeadline = (short) 100;
        this.lastKnownBlock = 0;
        this.unconfirmedPoolDepositAtm = 100 * Constants.ONE_APL;
        this.shufflingDepositAtm = 1000 * Constants.ONE_APL;
        this.guaranteedBalanceConfirmations = 1440;

        this.enablePruning = maxPrunableLifetime >= 0;
        this.maxPrunableLifetime = enablePruning ? Math.max(maxPrunableLifetime, minPrunableLifetime) : Integer.MAX_VALUE;
    }

    public BlockchainConfig(Chain chain, PropertiesHolder holder) {
        updateChain(chain, holder);
    }

    void updateChain(Chain chain, PropertiesHolder holder) {
        int maxPrunableLifetime = holder.getIntProperty("apl.maxPrunableLifetime");
        updateChain(chain, maxPrunableLifetime);
    }

    void updateChain(Chain chain) {
        updateChain(chain, 0);
    }

    public String getProjectName() {
        return chain.getProject();
    }

    public String getAccountPrefix() {
        return chain.getPrefix();
    }

    public String getCoinSymbol() {
        return chain.getSymbol();
    }

    public int getLeasingDelay() {
        return leasingDelay;
    }

    public int getMinPrunableLifetime() {
        return minPrunableLifetime;
    }

    public short getShufflingProcessingDeadline() {
        return shufflingProcessingDeadline;
    }

    public long getLastKnownBlock() {
        return lastKnownBlock;
    }

    public long getUnconfirmedPoolDepositAtm() {
        return unconfirmedPoolDepositAtm;
    }

    public long getShufflingDepositAtm() {
        return shufflingDepositAtm;
    }

    public int getGuaranteedBalanceConfirmations() {
        return guaranteedBalanceConfirmations;
    }

    public boolean isEnablePruning() {
        return enablePruning;
    }

    public int getMaxPrunableLifetime() {
        return maxPrunableLifetime;
    }

    public HeightConfig getCurrentConfig() {
        return currentConfig;
    }

    public Chain getChain() {
        return chain;
    }

    /**
     * For UNIT TEST only!
     * @param currentConfig
     */
    public void setCurrentConfig(HeightConfig currentConfig) {
        this.currentConfig = currentConfig;
    }
}
