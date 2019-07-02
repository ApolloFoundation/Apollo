/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShardDTO {

    public Long shardId;
    public String shardHash;
    public Long shardState;
    public Integer shardHeight;
    public String zipHashCrc;
    public String generatorIds;

}
