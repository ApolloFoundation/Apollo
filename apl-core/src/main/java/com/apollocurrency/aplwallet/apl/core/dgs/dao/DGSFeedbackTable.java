/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.dao;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dgs.EncryptedDataUtil;
import com.apollocurrency.aplwallet.apl.core.dgs.mapper.DGSFeedbackMapper;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSFeedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public class DGSFeedbackTable extends VersionedDeletableValuesDbTable<DGSFeedback> {
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
    private static final DGSFeedbackMapper MAPPER = new DGSFeedbackMapper(KEY_FACTORY);

    public DGSFeedbackTable() {
        super(TABLE_NAME, false, KEY_FACTORY);
    }

    @Override
    public DGSFeedback load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        DGSFeedback feedback = MAPPER.map(rs, null);
        feedback.setDbKey(dbKey);
        return feedback;
    }

    @Override
    public void save(Connection con, DGSFeedback feedback) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO purchase_feedback (id, feedback_data, feedback_nonce, "
                + "height, latest) VALUES (?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, feedback.getPurchaseId());
            i = EncryptedDataUtil.setEncryptedData(pstmt, feedback.getFeedbackEncryptedData(), ++i);
            pstmt.setInt(i, feedback.getHeight());
            pstmt.executeUpdate();
        }
    }

    public List<DGSFeedback> get(long id) {
        return get(KEY_FACTORY.newKey(id));
    }
}
