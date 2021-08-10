/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.util.List;

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
    public List<Block> getBlocksByAccount(long accountId, int from, int to, int timestamp) {
        return lookupBlockchain().getBlocksByAccountFromShards(accountId, from, to, timestamp);
    }

    @Override
    public int getLastBlockTimestamp() {
        return lookupBlockchain().getLastBlockTimestamp();
    }

    @Override
    public boolean isTrimming(){
        return lookupBlockchainProcessor().isTrimming();
    }

}
