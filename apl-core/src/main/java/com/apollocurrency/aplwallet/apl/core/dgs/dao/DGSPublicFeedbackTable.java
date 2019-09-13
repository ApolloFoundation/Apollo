/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.dao;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dgs.mapper.DGSPublicFeedbackMapper;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPublicFeedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public class DGSPublicFeedbackTable extends VersionedDeletableValuesDbTable<DGSPublicFeedback> {
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
        super(TABLE_NAME, false, KEY_FACTORY);
    }

    private static final DGSPublicFeedbackMapper MAPPER = new DGSPublicFeedbackMapper(KEY_FACTORY);
    @Override
    public DGSPublicFeedback load(Connection connection, ResultSet rs, DbKey dbKey) throws SQLException {
        return MAPPER.map(rs, null);
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

    public List<DGSPublicFeedback> get(long id) {
        return get(KEY_FACTORY.newKey(id));
    }
}
