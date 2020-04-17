/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs;

import com.apollocurrency.aplwallet.apl.core.db.DbClause;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DGSPurchasesClause extends DbClause {

    public DGSPurchasesClause(String clause, boolean withPublicFeedbacksOnly, boolean completedOnly) {
        super(clause + (completedOnly ? " AND goods IS NOT NULL " : " ")
            + (withPublicFeedbacksOnly ? " AND has_public_feedbacks = TRUE " : " "));
    }

    @Override
    public int set(PreparedStatement pstmt, int index) throws SQLException {
        return index;
    }
}
