/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Arrays;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

@ApplicationScoped
public class DefaultBlockValidator extends AbstractBlockValidator {

    @Inject
    public DefaultBlockValidator(BlockDb blockDb, BlockchainConfig blockchainConfig) {
        super(blockDb, blockchainConfig);
    }

    @Override
    void validatePreviousHash(BlockImpl block, BlockImpl previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (!Arrays.equals(Crypto.sha256().digest(previousBlock.bytes()),
                block.getPreviousBlockHash())) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Previous block hash doesn't match", block);
        }
    }

    @Override
    void verifySignature(BlockImpl block) throws BlockchainProcessor.BlockNotAcceptedException {
        if (!block.verifyBlockSignature()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Block signature verification failed", block);
        }
    }

    @Override
    void validateAdaptiveBlock(BlockImpl block, BlockImpl previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        int actualBlockTime = block.getTimestamp() - previousBlock.getTimestamp();
        if (actualBlockTime < blockchainConfig.getCurrentConfig().getAdaptiveBlockTime() && block.getTransactions().size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid adaptive block. " + actualBlockTime, null);
        }
    }

    @Override
    void validateInstantBlock(BlockImpl block, BlockImpl previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (block.getTransactions().size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Incorrect instant block", block);
        }
    }

    @Override
    void validateRegularBlock(BlockImpl block, BlockImpl previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (block.getTransactions().size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock() || block.getTimeout() != 0) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Incorrect regular block", block);
        }
    }
}
