/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShardDTO {

    public Long shardId;
    public String shardHash;
    public Long shardState;
    public Integer shardHeight;
    public String coreZipHash;
    public String prunableZipHash;
    public String generatorIds;
    public String blockTimeouts;
    public String blockTimestamps;

}
