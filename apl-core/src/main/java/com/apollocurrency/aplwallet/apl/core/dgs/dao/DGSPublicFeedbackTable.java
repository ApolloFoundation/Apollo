/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.dao;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;

@Singleton
public class DGSPublicFeedbackTable extends VersionedValuesDbTable<DGSPublicFeedback> {
    private static final LongKeyFactory<DGSPublicFeedback> KEY_FACTORY = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(DGSPublicFeedback publicFeedback) {
            if (publicFeedback.getDbKey() == null) {
                DbKey dbKey = newKey(publicFeedback.getId());
                publicFeedback.setDbKey(dbKey);
            }
            return publicFeedback.getDbKey();
        }

    };
    private static final String TABLE_NAME = "purchase_public_feedback";

    protected DGSPublicFeedbackTable() {
        super(TABLE_NAME, KEY_FACTORY);
    }

    @Override
    public DGSPublicFeedback load(Connection connection, ResultSet rs, DbKey dbKey) throws SQLException {
        String feedback = rs.getString("public_feedback");
        int height = rs.getInt("height");
        long purchaseId = rs.getLong("id");
        return new DGSPublicFeedback(feedback, purchaseId, height);
    }

    @Override
    public void save(Connection con,  DGSPublicFeedback feedback) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO purchase_public_feedback (id, public_feedback, "
                + "height, latest) VALUES (?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, feedback.getId());
            pstmt.setString(++i, feedback.getFeedback());
            pstmt.setInt(++i, feedback.getHeight());
            pstmt.executeUpdate();
        }
    }

}
