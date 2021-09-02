/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

import com.apollocurrency.aplwallet.apl.core.converter.db.dgs.DGSFeedbackMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSFeedback;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.utils.EncryptedDataUtil;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Singleton
public class DGSFeedbackTable extends ValuesDbTable<DGSFeedback> {
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

    @Inject
    public DGSFeedbackTable(DatabaseManager databaseManager,
                            Event<FullTextOperationData> fullTextOperationDataEvent) {
        super(TABLE_NAME, KEY_FACTORY, true,
            databaseManager, fullTextOperationDataEvent);
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
            + "height, latest) VALUES (?, ?, ?, ?, TRUE)", Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setLong(++i, feedback.getPurchaseId());
            i = EncryptedDataUtil.setEncryptedData(pstmt, feedback.getFeedbackEncryptedData(), ++i);
            pstmt.setInt(i, feedback.getHeight());
            pstmt.executeUpdate();
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    feedback.setDbId(rs.getLong(1));
                }
            }
        }
    }

    public List<DGSFeedback> get(long id) {
        return get(KEY_FACTORY.newKey(id));
    }
}
