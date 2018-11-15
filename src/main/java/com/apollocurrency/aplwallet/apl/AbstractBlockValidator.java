/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

public abstract class AbstractBlockValidator implements BlockValidator {
    private static final Logger LOG = getLogger(AbstractBlockValidator.class);

    @Override
    public void validate(BlockImpl block, BlockImpl previousLastBlock, int curTime) throws BlockchainProcessor.BlockNotAcceptedException {
        if (previousLastBlock.getId() != block.getPreviousBlockId()) {
            throw new BlockchainProcessor.BlockOutOfOrderException("Previous block id doesn't match", block);
        }
        if (block.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT) {
            LOG.warn("Received block " + block.getStringId() + " from the future, timestamp " + block.getTimestamp()
                    + " generator " + Long.toUnsignedString(block.getGeneratorId()) + " current time " + curTime + ", system clock may be off");
            throw new BlockchainProcessor.BlockOutOfOrderException("Invalid timestamp: " + block.getTimestamp()
                    + " current time is " + curTime, block);
        }
        if (block.getTimestamp() <= previousLastBlock.getTimestamp()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Block timestamp " + block.getTimestamp() + " is before previous block timestamp "
                    + previousLastBlock.getTimestamp(), block);
        }
//        if (!block.verifyBlockSignature()) {
//            throw new BlockchainProcessor.BlockNotAcceptedException("Block signature verification failed", block);
//        }
        verifySignature(block);
        validatePreviousHash();
        if (block.getId() == 0L || BlockDb.hasBlock(block.getId(), previousLastBlock.getHeight())) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Duplicate block or invalid id", block);
        }
        if (!block.verifyGenerationSignature() && !Generator.allowsFakeForging(block.getGeneratorPublicKey())) {
            Account generatorAccount = Account.getAccount(block.getGeneratorId());
            long generatorBalance = generatorAccount == null ? 0 : generatorAccount.getEffectiveBalanceAPL();
            throw new BlockchainProcessor.BlockNotAcceptedException("Generation signature verification failed, effective balance " + generatorBalance, block);
        }

        if (block.getTransactions().size() > Constants.getMaxNumberOfTransactions()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid block transaction count " + block.getTransactions().size(), block);
        }
        if (block.getPayloadLength() > Constants.getMaxPayloadLength() || block.getPayloadLength() < 0) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid block payload length " + block.getPayloadLength(), block);
        }
        validateAdaptiveBlock(block, previousLastBlock);



//        int actualBlockTime = block.getTimestamp() - previousLastBlock.getTimestamp();
//        if (Constants.isAdaptiveBlockAtHeight(previousLastBlock.getHeight() + 1) && actualBlockTime < Constants.getAdaptiveForgingEmptyBlockTime() && block.getTransactions().size() == 0) {
//            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid empty block. Time since previous block should be greater than " + Constants.getAdaptiveForgingEmptyBlockTime() + ", but " +
//                    "got " + actualBlockTime, null);
//        }

    }

    abstract void validatePreviousHash(BlockImpl block, BlockImpl previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void verifySignature(BlockImpl block);

    abstract void validateAdaptiveBlock(BlockImpl block, BlockImpl previousBlock);

}
