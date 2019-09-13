/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultBlockValidator extends AbstractBlockValidator {

    @Inject
    public DefaultBlockValidator(Blockchain blockchain, BlockchainConfig blockchainConfig) {
        super(blockchain, blockchainConfig);
    }

    @Override
    void validatePreviousHash(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (!Arrays.equals(Crypto.sha256().digest(((BlockImpl)previousBlock).bytes()),
                block.getPreviousBlockHash())) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Previous block hash doesn't match", block);
        }
    }

    @Override
    void verifySignature(Block block) throws BlockchainProcessor.BlockNotAcceptedException {
        if (!block.verifyBlockSignature()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Block signature verification failed", block);
        }
    }

    @Override
    void validateAdaptiveBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        int actualBlockTime = block.getTimestamp() - previousBlock.getTimestamp();
        if (actualBlockTime < blockchainConfig.getCurrentConfig().getAdaptiveBlockTime() && block.getOrLoadTransactions().size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid adaptive block: time - " + actualBlockTime + " height " + previousBlock.getHeight() + 1 + ". Perhaps blockchain config is outdated", null);
        }
    }

    @Override
    void validateInstantBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (block.getOrLoadTransactions().size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Incorrect instant block", block);
        }
    }

    @Override
    void validateRegularBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (block.getOrLoadTransactions().size() <= blockchainConfig.getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock() || block.getTimeout() != 0) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Incorrect regular block", block);
        }
    }
}
