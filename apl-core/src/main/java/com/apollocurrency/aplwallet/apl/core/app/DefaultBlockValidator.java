/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import javax.enterprise.context.ApplicationScoped;
import java.util.Arrays;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;

@ApplicationScoped
public class DefaultBlockValidator extends AbstractBlockValidator {

    public DefaultBlockValidator() { // for weld
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
        if (actualBlockTime < AplGlobalObjects.getChainConfig().getCurrentConfig().getAdaptiveBlockTime() && block.getTransactions().size() <= AplGlobalObjects.getChainConfig().getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid adaptive block. " + actualBlockTime, null);
        }
    }

    @Override
    void validateInstantBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (block.getTransactions().size() <= AplGlobalObjects.getChainConfig().getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Incorrect instant block", block);
        }
    }

    @Override
    void validateRegularBlock(Block block, Block previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (block.getTransactions().size() <= AplGlobalObjects.getChainConfig().getCurrentConfig().getNumberOfTransactionsInAdaptiveBlock() || block.getTimeout() != 0) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Incorrect regular block", block);
        }
    }
}
