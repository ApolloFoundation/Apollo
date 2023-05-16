/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs.dbclause;


import com.apollocurrency.aplwallet.apl.util.db.DbClause;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SellerDbClause extends DbClause {

    private final long sellerId;

    public SellerDbClause(long sellerId, boolean inStockOnly) {
        super(" seller_id = ? " + (inStockOnly ? "AND delisted = FALSE AND quantity > 0" : ""));
        this.sellerId = sellerId;
    }

    @Override
    public int set(PreparedStatement pstmt, int index) throws SQLException {
        pstmt.setLong(index++, sellerId);
        return index;
    }

}