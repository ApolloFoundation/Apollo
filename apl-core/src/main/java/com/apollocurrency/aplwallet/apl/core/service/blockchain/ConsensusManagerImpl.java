/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;

import jakarta.inject.Singleton;
import java.math.BigInteger;
import java.util.Objects;

@Singleton
public class ConsensusManagerImpl implements ConsensusManager {

    @Override
    public void setPrevious(Block currentBlock, Block[] threeLatestBlocksArray, HeightConfig config, Shard lastShard, int initialBlockHeight) {
        Objects.requireNonNull(config, "HeightConfig is NULL");
        Objects.requireNonNull(threeLatestBlocksArray, "shardInitialBlock is NULL");// check for now (not sure if needed?)
        if (threeLatestBlocksArray.length == 0) {
            // shouldn't happen as previous id is already verified, but just in case
            throw new IllegalArgumentException("threeLatestBlocksArray is empty and has 0 element(s)");
        }
        if (threeLatestBlocksArray[0] != null) {
            if (threeLatestBlocksArray[0].getId() != currentBlock.getPreviousBlockId()) {
                // shouldn't happen as previous id is already verified, but just in case
                throw new IllegalStateException("Previous threeLatestBlocksArray id doesn't match");
            }
            currentBlock.setHeight( threeLatestBlocksArray[0].getHeight() + 1 );
            this.calculateBaseTarget(currentBlock, threeLatestBlocksArray, config, lastShard, initialBlockHeight);
        } else {
            currentBlock.setHeight(0);
        }
    }

    private void calculateBaseTarget(Block currentBlock, Block[] threeLatestBlocksArray, HeightConfig config, Shard lastShard, int initialBlockHeight) {
        long prevBaseTarget = threeLatestBlocksArray[0].getBaseTarget();
        int blockchainHeight = threeLatestBlocksArray[0].getHeight();
        if (blockchainHeight > 2 && blockchainHeight % 2 == 0) {
            int blocktimeAverage = getBlockTimeAverage(currentBlock, threeLatestBlocksArray, lastShard, initialBlockHeight);
            int blockTime = config.getBlockTime();
            if (blocktimeAverage > blockTime) {
                int maxBlocktimeLimit = config.getMaxBlockTimeLimit();
                currentBlock.setBaseTarget( (prevBaseTarget * Math.min(blocktimeAverage, maxBlocktimeLimit)) / blockTime );
            } else {
                int minBlocktimeLimit = config.getMinBlockTimeLimit();
                currentBlock.setBaseTarget( prevBaseTarget - prevBaseTarget * Constants.BASE_TARGET_GAMMA
                    * (blockTime - Math.max(blocktimeAverage, minBlocktimeLimit)) / (100 * blockTime) );
            }
            long maxBaseTarget = config.getMaxBaseTarget();
            if (currentBlock.getBaseTarget() < 0 || currentBlock.getBaseTarget() > maxBaseTarget) {
                currentBlock.setBaseTarget( maxBaseTarget );
            }
            long minBaseTarget = config.getMinBaseTarget();
            if (currentBlock.getBaseTarget() < minBaseTarget) {
                currentBlock.setBaseTarget( config.getMinBaseTarget() );
            }
        } else {
            currentBlock.setBaseTarget( prevBaseTarget );
        }
        currentBlock.setCumulativeDifficulty(
            threeLatestBlocksArray[0].getCumulativeDifficulty().add(Convert.two64.divide(
                BigInteger.valueOf(currentBlock.getBaseTarget())))
        );
    }

    private int getBlockTimeAverage(Block currentBlock, Block[] threeLatestBlocksArray, Shard lastShard, int initialBlockHeight) {
        int blockchainHeight = threeLatestBlocksArray[0].getHeight();
        Block blockAtHeight = threeLatestBlocksArray[2];
        int lastBlockTimestamp = validatePrevTimestamp(
            initialBlockHeight, blockchainHeight - 2, lastShard, blockAtHeight);
        if (currentBlock.getVersion() != Block.LEGACY_BLOCK_VERSION) {
            blockAtHeight = threeLatestBlocksArray[1];
            int intermediateTimestamp = validatePrevTimestamp(initialBlockHeight, blockchainHeight - 1, lastShard, blockAtHeight);
            int intermediateTimeout = validatePrevTimeout(initialBlockHeight, blockchainHeight - 1, lastShard, blockAtHeight);
            int thisBlockActualTime = currentBlock.getTimestamp() - threeLatestBlocksArray[0].getTimestamp() - currentBlock.getTimeout();
            int previousBlockTime = threeLatestBlocksArray[0].getTimestamp() - threeLatestBlocksArray[0].getTimeout() - intermediateTimestamp;
            int secondAvgBlockTime = intermediateTimestamp
                - intermediateTimeout - lastBlockTimestamp;
            return (thisBlockActualTime + previousBlockTime + secondAvgBlockTime) / 3;
        } else {
            return (currentBlock.getTimestamp() - lastBlockTimestamp) / 3;
        }
    }

    private int validatePrevTimestamp(int shardInitialHeight, int blockHeight, Shard lastShard, Block blockAtHeight) {
        int diff = shardInitialHeight - blockHeight;
        if (diff > 2) {
            throw new IllegalArgumentException("Unable to retrieve block timestamp for height " + blockHeight + " current shard height " + shardInitialHeight);
        }
        if (diff > 0) {
            int[] blockTimestamps = lastShard.getBlockTimestamps();
            return blockTimestamps[diff - 1];
        }
        return blockAtHeight.getTimestamp();
    }

    private int validatePrevTimeout(int shardInitialHeight, int blockHeight, Shard lastShard, Block blockAtHeight) {
        int diff = shardInitialHeight - blockHeight;
        if (diff > 2) {
            throw new IllegalArgumentException("Unable to retrieve block timeout for height " + blockHeight + " current shard height " + shardInitialHeight);
        }
        if (diff > 0) {
            int[] blockTimeouts = lastShard.getBlockTimeouts();
            return blockTimeouts[diff - 1];
        }
        return blockAtHeight.getTimeout();
    }




}
