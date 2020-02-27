/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.service;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;

/**
 * @author silaev-firstbridge on 1/31/2020
 */
@Singleton
@NoArgsConstructor
@AllArgsConstructor
public class BlockChainInfoServiceImpl implements BlockChainInfoService {
    private volatile Blockchain blockchain;
    private volatile BlockchainProcessor blockchainProcessor;

    private Blockchain lookupBlockchain() {
        if (blockchain == null) blockchain = CDI.current().select(Blockchain.class).get();
        return blockchain;
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        return blockchainProcessor;
    }

    @Override
    public boolean doesNotExceed(int height) {
        return lookupBlockchain().getHeight() <= height;
    }

    @Override
    public void checkAvailable(int height, boolean isMultiVersion) {
        if (isMultiVersion) {
            int minRollBackHeight = lookupBlockchainProcessor().getMinRollbackHeight();
            if (height < minRollBackHeight) {
                throw new IllegalArgumentException("Historical data as of height " + height + " not available.");
            }
        }

        if (height > lookupBlockchain().getHeight()) {
            throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + lookupBlockchain().getHeight());
        }
    }

    /**
     * Serves AcconutAssetService, AccountService and Asset(to refactor)
     *
     * @param height
     */
    @Override
    public void checkAvailable(int height) {
        if (height < lookupBlockchainProcessor().getMinRollbackHeight()) {
            throw new IllegalArgumentException("Historical data as of height " + height + " not available.");
        }

        if (height > lookupBlockchain().getHeight()) {
            throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + lookupBlockchain().getHeight());
        }
    }

    @Override
    public int getMinRollbackHeight() {
        return lookupBlockchainProcessor().getMinRollbackHeight();
    }

    @Override
    public int getHeight() {
        return lookupBlockchain().getHeight();
    }

    @Override
    public Block getLastBlock() {
        return lookupBlockchain().getLastBlock();
    }

    @Override
    public DbIterator<Block> getBlocks(long accountId, int timestamp, int from, int to) {
        return blockchain.getBlocks(accountId, timestamp, from, to);
    }
}
