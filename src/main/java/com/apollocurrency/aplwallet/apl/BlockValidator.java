package com.apollocurrency.aplwallet.apl;

public interface BlockValidator {
    void validate(BlockImpl block, BlockImpl previousLastBlock, int curTime) throws BlockchainProcessor.BlockNotAcceptedException;
}
