/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.model.Block;

public interface ConsensusManager {

    void setPrevious(Block currentBlock, Block[] threeLatestBlocksArray, HeightConfig config, Shard lastShard, int initialBlockHeight);

}
