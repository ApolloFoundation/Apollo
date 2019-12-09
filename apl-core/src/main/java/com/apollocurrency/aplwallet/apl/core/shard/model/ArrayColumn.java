package com.apollocurrency.aplwallet.apl.core.shard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author silaev-firstbridge on 12/9/2019
 */
@EqualsAndHashCode(of = {"tableName", "columnName"})
@Builder
@Getter
@AllArgsConstructor
public class ArrayColumn {
    final String tableName;
    final String columnName;
    final int precision;
    final int scale;
}