/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.poll;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.PollOptionResult;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

@Singleton
@Slf4j
public final class PollResultTable extends ValuesDbTable<PollOptionResult> {
    private static final LongKeyFactory<PollOptionResult> POLL_RESULTS_DB_KEY_FACTORY = new LongKeyFactory<>("poll_id") {
        @Override
        public DbKey newKey(PollOptionResult pollOptionResult) {
            if (pollOptionResult.getDbKey() == null) {
                pollOptionResult.setDbKey(new LongKey(pollOptionResult.getPollId()));
            }
            return pollOptionResult.getDbKey();
        }
    };

    @Inject
    public PollResultTable(DatabaseManager databaseManager,
                           Event<FullTextOperationData> fullTextOperationDataEvent) {
        super("poll_result", POLL_RESULTS_DB_KEY_FACTORY,
            false, databaseManager, fullTextOperationDataEvent);
    }

    @Override
    public PollOptionResult load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        long id = rs.getLong("poll_id");
        long result = rs.getLong("result");
        long weight = rs.getLong("weight");
        return weight == 0 ? new PollOptionResult(id) : new PollOptionResult(id, result, weight);
    }

    @Override
    protected void save(Connection con, PollOptionResult optionResult) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO poll_result (poll_id, "
            + "result, weight, height) VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, optionResult.getPollId());
            if (!optionResult.isUndefined()) {
                pstmt.setLong(++i, optionResult.getResult());
                pstmt.setLong(++i, optionResult.getWeight());
            } else {
                pstmt.setNull(++i, Types.BIGINT);
                pstmt.setLong(++i, 0);
            }
            int height = optionResult.getHeight();
            pstmt.setInt(++i, optionResult.getHeight());
            log.trace("PollResult save = {} at height = {}", optionResult, height);
            pstmt.executeUpdate();
        }
    }

    public DbKey getDbKey(long id) {
        return POLL_RESULTS_DB_KEY_FACTORY.newKey(id);
    }
}
