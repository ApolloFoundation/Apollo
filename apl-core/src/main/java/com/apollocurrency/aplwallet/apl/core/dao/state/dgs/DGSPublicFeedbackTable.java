/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

import com.apollocurrency.aplwallet.apl.core.converter.db.dgs.DGSPublicFeedbackMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    protected DGSPublicFeedbackTable(DerivedTablesRegistry derivedDbTablesRegistry,
                                     DatabaseManager databaseManager,
                                     Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super(TABLE_NAME, KEY_FACTORY, true, derivedDbTablesRegistry, databaseManager, null, deleteOnTrimDataEvent);
    }

    @Override
    public DGSPublicFeedback load(Connection connection, ResultSet rs, DbKey dbKey) throws SQLException {
        return MAPPER.map(rs, null);
    }

    @Override
    public void save(Connection con, DGSPublicFeedback feedback) throws SQLException {
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
