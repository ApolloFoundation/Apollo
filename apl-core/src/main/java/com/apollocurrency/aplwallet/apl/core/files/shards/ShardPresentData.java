/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.files.shards;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShardPresentData {
    public Long shardId;
    public String shardFileId; // contains shardId + chainId in special format
    public List<String> additionalFileIDs;
}
