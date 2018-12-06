package com.apollocurrency.aplwallet.apl.core.app;

public interface BlockValidator {
    void validate(BlockImpl block, BlockImpl previousLastBlock, int curTime) throws BlockchainProcessor.BlockNotAcceptedException;
}
