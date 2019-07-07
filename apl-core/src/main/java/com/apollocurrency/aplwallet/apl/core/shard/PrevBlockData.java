package com.apollocurrency.aplwallet.apl.core.shard;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PrevBlockData {
    private Long[] generatorIds; // 3 generator ids before snapshot block with height offset (-1, -2, -3)
    private Integer[] prevBlockTimestamps; // timestamps for blocks before shard block to maintain consensus
    private Integer[] prevBlockTimeouts; // timeouts for blocks before shard block to maintain consensus
}
