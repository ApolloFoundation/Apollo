/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.dao;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dgs.DGSFeedback;
import com.apollocurrency.aplwallet.apl.core.dgs.EncryptedDataUtil;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.inject.Singleton;

@Singleton
public class DGSFeedbackTable extends VersionedValuesDbTable<DGSFeedback> {
    private static final LongKeyFactory<DGSFeedback> KEY_FACTORY = new LongKeyFactory<DGSFeedback>("id") {
        @Override
        public DbKey newKey(DGSFeedback feedback) {
            if (feedback.getDbKey() == null) {
                feedback.setDbKey(new LongKey(feedback.getPurchaseId()));
            }
            return feedback.getDbKey();
        }
    };
    private static final String TABLE_NAME = "purchase_feedback";


    public DGSFeedbackTable() {
        super(TABLE_NAME, KEY_FACTORY);
    }

    @Override
    public DGSFeedback load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        byte[] data = rs.getBytes("feedback_data");
        byte[] nonce = rs.getBytes("feedback_nonce");
        long id = rs.getLong("id");
        int height = rs.getInt("height");
        return new DGSFeedback(id, height, new EncryptedData(data, nonce));
    }

    @Override
    public void save(Connection con, DGSFeedback feedback) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO purchase_feedback (id, feedback_data, feedback_nonce, "
                + "height, latest) VALUES (?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, feedback.getPurchaseId());
//            i = EncryptedDataUtil.setEncryptedData(pstmt, feedback.getFeedbackEncryptedData(), ++i);
            i = EncryptedDataUtil.setEncryptedData(pstmt, feedback.getFeedbackEncryptedData(), ++i);
//            pstmt.setInt(i, feedback.getHeight());
            pstmt.executeUpdate();

        }
    }
}
