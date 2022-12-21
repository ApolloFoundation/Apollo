/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

import com.apollocurrency.aplwallet.apl.core.converter.db.dgs.DGSPublicFeedbackMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Singleton
public class DGSPublicFeedbackTable extends ValuesDbTable<DGSPublicFeedback> {
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
    private static final DGSPublicFeedbackMapper MAPPER = new DGSPublicFeedbackMapper(KEY_FACTORY);

    @Inject
    protected DGSPublicFeedbackTable(DatabaseManager databaseManager,
                                     Event<FullTextOperationData> fullTextOperationDataEvent) {
        super(TABLE_NAME, KEY_FACTORY, true, databaseManager, fullTextOperationDataEvent);
    }

    @Override
    public DGSPublicFeedback load(Connection connection, ResultSet rs, DbKey dbKey) throws SQLException {
        return MAPPER.map(rs, null);
    }

    @Override
    public void save(Connection con, DGSPublicFeedback feedback) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO purchase_public_feedback (id, public_feedback, "
            + "height, latest) VALUES (?, ?, ?, TRUE)", Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setLong(++i, feedback.getId());
            pstmt.setString(++i, feedback.getFeedback());
            pstmt.setInt(++i, feedback.getHeight());
            pstmt.executeUpdate();
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    feedback.setDbId(rs.getLong(1));
                }
            }
        }
    }

    public List<DGSPublicFeedback> get(long id) {
        return get(KEY_FACTORY.newKey(id));
    }
}
