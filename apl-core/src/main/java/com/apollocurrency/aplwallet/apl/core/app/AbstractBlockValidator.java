/*
 * Copyright © 2018-2019 Apollo Foundation
 */


package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.db.BlockDao;
import com.apollocurrency.aplwallet.apl.util.Constants;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import org.slf4j.Logger;

import java.util.Objects;
import javax.inject.Inject;

public abstract class AbstractBlockValidator implements BlockValidator {
    private static final Logger LOG = getLogger(AbstractBlockValidator.class);
    private BlockDao blockDao;
    protected BlockchainConfig blockchainConfig;
    
    @Inject
    public AbstractBlockValidator(BlockDao blockDao, BlockchainConfig blockchainConfig) {
        Objects.requireNonNull(blockDao, "BlockDao is null");
        Objects.requireNonNull(blockchainConfig, "Blockchain config is null");
        this.blockDao = blockDao;
        this.blockchainConfig = blockchainConfig;
    }

    @Override
    public void validate(Block block, Block previousLastBlock, int curTime) throws BlockchainProcessor.BlockNotAcceptedException {
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
        if (block.getId() == 0L || blockDao.hasBlock(block.getId(), previousLastBlock.getHeight())) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Duplicate block or invalid id", block);
        }
        if (!block.verifyGenerationSignature()) {
            Account generatorAccount = Account.getAccount(block.getGeneratorId());
            long generatorBalance = generatorAccount == null ? 0 : generatorAccount.getEffectiveBalanceAPL();
            throw new BlockchainProcessor.BlockNotAcceptedException("Generation signature verification failed, effective balance " + generatorBalance, block);
        }

        if (block.getTransactions().size() > blockchainConfig.getCurrentConfig().getMaxNumberOfTransactions()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid block transaction count " + block.getTransactions().size(), block);
        }
        if (block.getPayloadLength() > blockchainConfig.getCurrentConfig().getMaxPayloadLength() || block.getPayloadLength() < 0) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid block payload length " + block.getPayloadLength(), block);
        }
        switch (block.getVersion()) {
            case Block.LEGACY_BLOCK_VERSION:
                if (blockchainConfig.getCurrentConfig().isAdaptiveForgingEnabled()) {
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

    abstract void validatePreviousHash(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void verifySignature(Block block) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void validateAdaptiveBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void validateInstantBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

    abstract void validateRegularBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException;

}
