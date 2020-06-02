/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata.dgs.dbclause;

import com.apollocurrency.aplwallet.apl.core.utils.DGSPurchasesClause;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class SellerBuyerDGSPurchasesClause extends DGSPurchasesClause {

    private final long sellerId;
    private final long buyerId;

    public SellerBuyerDGSPurchasesClause(long sellerId, long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly) {
        super(" seller_id = ? AND buyer_id = ? ", withPublicFeedbacksOnly, completedOnly);
        this.sellerId = sellerId;
        this.buyerId = buyerId;
    }

    @Override
    public int set(PreparedStatement pstmt, int index) throws SQLException {
        pstmt.setLong(index++, sellerId);
        pstmt.setLong(index++, buyerId);
        return index;
    }

}
