/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class LongDGSPurchasesClause extends DGSPurchasesClause {

    private final long value;

    public LongDGSPurchasesClause(String columnName, long value, boolean withPublicFeedbacksOnly, boolean completedOnly) {
        super(columnName + " = ? ", withPublicFeedbacksOnly, completedOnly);
        this.value = value;
    }

    @Override
    protected int set(PreparedStatement pstmt, int index) throws SQLException {
        pstmt.setLong(index++, value);
        return index;
    }

}