/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class DeleteOnTrimData {
    private boolean isResetEvent; // signal to clean up queue
    private Set<Long> dbIdSet = new HashSet<>(0); // set fo ids to delete
    private String tableName; // table to be deleted

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DeleteOnTrimData{");
        sb.append("isResetEvent=").append(isResetEvent);
        sb.append(", tableName='").append(tableName).append('\'');
        sb.append(", dbIdSet=[").append(dbIdSet.size()).append("]");
        sb.append('}');
        return sb.toString();
    }
}
