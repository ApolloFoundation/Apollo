/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;

public interface BlockValidator {
    void validate(Block block, Block previousLastBlock, int curTime) throws BlockchainProcessor.BlockNotAcceptedException;
}
