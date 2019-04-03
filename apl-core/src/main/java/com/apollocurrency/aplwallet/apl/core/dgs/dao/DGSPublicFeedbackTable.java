/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.dao;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.VersionedValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DGSPublicFeedbackTable {
    private static final LongKeyFactory<DGSPublicFeedback> publicFeedbackDbKeyFactory = new LongKeyFactory<>("id") {

        @Override
        public DbKey newKey(DGSPublicFeedback publicFeedback) {
            return purchase.dbKey == null ? newKey(purchase.id) : purchase;
        }

    };

    private static final VersionedValuesDbTable<DGSPurchase, String> publicFeedbackTable = new VersionedValuesDbTable<DGSPurchase, String>("purchase_public_feedback", publicFeedbackDbKeyFactory) {

        @Override
        protected String load(Connection con, ResultSet rs) throws SQLException {
            return rs.getString("public_feedback");
        }

        @Override
        protected void save(Connection con, DGSPurchase purchase, String publicFeedback) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO purchase_public_feedback (id, public_feedback, "
                    + "height, latest) VALUES (?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, purchase.getId());
                pstmt.setString(++i, publicFeedback);
                pstmt.setInt(++i, blockchain.getHeight());
                pstmt.executeUpdate();
            }
        }

    };
}
