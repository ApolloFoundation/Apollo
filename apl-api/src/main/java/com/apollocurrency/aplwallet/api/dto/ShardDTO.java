/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
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
