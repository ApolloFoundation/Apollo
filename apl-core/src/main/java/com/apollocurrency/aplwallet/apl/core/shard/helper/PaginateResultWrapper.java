/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

/**
 * Internal wrapper class is used for paginating on tables by select sql statement.
 *
 * @author yuriy.larin
 */
class PaginateResultWrapper {

    public Long lowerBoundColumnValue = -1L; // previous column value will be used for next select query
    public Long upperBoundColumnValue = -1L; // usually unchanged
    public Boolean isFinished = Boolean.FALSE; // can be used as sign of ended result

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PaginateResultWrapper{");
        sb.append("lowerBoundColumnValue=").append(lowerBoundColumnValue);
        sb.append(", upperBoundColumnValue=").append(upperBoundColumnValue);
        sb.append(", isFinished=").append(isFinished);
        sb.append('}');
        return sb.toString();
    }
}
