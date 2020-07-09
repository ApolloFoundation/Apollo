/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.model;

import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class ExcludeInfo {
    private final List<TransactionDbInfo> deleteNotExportNotCopy;
    private final List<TransactionDbInfo> notDeleteExportNotCopy;
    private final List<TransactionDbInfo> notDeleteExportCopy;

    public Set<Long> getNotDeleteDbIds() {
        return Stream.concat(notDeleteExportCopy.stream().map(TransactionDbInfo::getDbId), notDeleteExportNotCopy.stream().map(TransactionDbInfo::getDbId)).collect(Collectors.toSet());
    }

    public Set<Long> getNotCopyDbIds() {
        return Stream.concat(deleteNotExportNotCopy.stream().map(TransactionDbInfo::getDbId), notDeleteExportNotCopy.stream().map(TransactionDbInfo::getDbId)).collect(Collectors.toSet());
    }

    public Set<Long> getExportDbIds() {
        return Stream.concat(notDeleteExportNotCopy.stream().map(TransactionDbInfo::getDbId), notDeleteExportCopy.stream().map(TransactionDbInfo::getDbId)).collect(Collectors.toSet());
    }
}
