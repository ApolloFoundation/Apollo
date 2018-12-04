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
        verifySignature(block);
        validatePreviousHash(block, previousLastBlock);
        if (block.getId() == 0L || BlockDb.hasBlock(block.getId(), previousLastBlock.getHeight())) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Duplicate block or invalid id", block);
        }
        if (!block.verifyGenerationSignature() && !Generator.allowsFakeForging(block.getGeneratorPublicKey())) {
            Account generatorAccount = Account.getAccount(block.getGeneratorId());
            long generatorBalance = generatorAccount == null ? 0 : generatorAccount.getEffectiveBalanceAPL();
            throw new BlockchainProcessor.BlockNotAcceptedException("Generation signature verification failed, effective balance " + generatorBalance, block);
        }

        if (block.getTransactions().size() > AplGlobalObjects.getChainConfig().getCurrentConfig().getMaxNumberOfTransactions()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid block transaction count " + block.getTransactions().size(), block);
        }
        if (block.getPayloadLength() > AplGlobalObjects.getChainConfig().getCurrentConfig().getMaxPayloadLength() || block.getPayloadLength() < 0) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid block payload length " + block.getPayloadLength(), block);
        }
        switch (block.getVersion()) {
            case Block.LEGACY_BLOCK_VERSION:
                if (AplGlobalObjects.getChainConfig().getCurrentConfig().isAdaptiveForgingEnabled()) {
                    throw new BlockchainProcessor.BlockNotAcceptedException("Legacy blocks are not accepting during adaptive forging", block);
                }
                break;
            case Block.INSTANT_BLOCK_VERSION:
                validateInstantBlock(block, previousLastBlock);
                break;
            case Block.ADAPTIVE_BLOCK_VERSION:
                validateAdaptiveBlock(block, previousLastBlock);
                break;
            case Block.REGULAR_BLOCK_VERSION:
                validateRegularBlock(block, previousLastBlock);
                break;
        }
    }

    abstract void validatePreviousHash(BlockImpl block, BlockImpl previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void verifySignature(BlockImpl block) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void validateAdaptiveBlock(BlockImpl block, BlockImpl previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void validateInstantBlock(BlockImpl block, BlockImpl previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void validateRegularBlock(BlockImpl block, BlockImpl previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

}
