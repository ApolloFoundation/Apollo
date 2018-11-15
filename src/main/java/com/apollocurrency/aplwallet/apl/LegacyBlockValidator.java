/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.util.Arrays;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;

public class DefaultBlockValidator extends AbstractBlockValidator {

    @Override
    void validatePreviousHash(BlockImpl block, BlockImpl previousBlock) throws BlockchainProcessor.BlockNotAcceptedException {
        if (!Arrays.equals(Crypto.sha256().digest(previousBlock.bytes(isAdaptive(), isLegacy())),
                block.getPreviousBlockHash())) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Previous block hash doesn't match", block);
        }
    }

    @Override
    void verifySignature(BlockImpl block) {
        if (!block.verifyBlockSignature()) {
            throw new BlockNotAcceptedException("Block signature verification failed", block);
        }
    }

    @Override
    void validateAdaptiveBlock(BlockImpl block, BlockImpl previousBlock) {

    }

    protected boolean isAdaptive() {
        return false;
    }

    protected boolean isLegacy() {
        return true;
    }
}
