/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.files.shards;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ShardPresentData {
    public Long shardId;
    public String shardFileId; // contains shardId + chainId in special format
    public List<String> additionalFileIDs;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ShardPresentData{");
        sb.append("shardId=").append(shardId);
        sb.append(", shardFileId='").append(shardFileId).append('\'');
        sb.append(", additionalFileIDs=").append(additionalFileIDs);
        sb.append('}');
        return sb.toString();
    }
}
