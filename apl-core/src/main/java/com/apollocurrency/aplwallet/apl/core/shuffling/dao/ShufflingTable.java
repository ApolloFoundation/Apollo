/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.shuffling.mapper.ShufflingMapper;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.inject.Singleton;

@Singleton
@Slf4j
public class ShufflingTable extends VersionedDeletableEntityDbTable<Shuffling> {
    private static final LongKeyFactory<Shuffling> KEY_FACTORY = new LongKeyFactory<>("id") {

        @Override
        public DbKey newKey(Shuffling shuffling) {
            if (shuffling.getDbKey() == null) {
                shuffling.setDbKey(new LongKey(shuffling.getId()));
            }
            return shuffling.getDbKey();
        }

    };
    private static final String TABLE_NAME = "shuffling";
    private static final ShufflingMapper MAPPER = new ShufflingMapper(KEY_FACTORY);

    public ShufflingTable() {
        super(TABLE_NAME, KEY_FACTORY, false);
    }

    @Override
    public Shuffling load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return MAPPER.map(rs, null);
    }


    @Override
    public void save(Connection con, Shuffling shuffling) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO shuffling (id, holding_id, holding_type, "
                + "issuer_id, amount, participant_count, blocks_remaining, stage, assignee_account_id, "
                + "recipient_public_keys, registrant_count, height, latest) "
                + "KEY (id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, shuffling.getId());
            DbUtils.setLongZeroToNull(pstmt, ++i, shuffling.getHoldingId());
            pstmt.setByte(++i, shuffling.getHoldingType().getCode());
            pstmt.setLong(++i, shuffling.getIssuerId());
            pstmt.setLong(++i, shuffling.getAmount());
            pstmt.setByte(++i, shuffling.getParticipantCount());
            DbUtils.setShortZeroToNull(pstmt, ++i, shuffling.getBlocksRemaining());
            pstmt.setByte(++i, shuffling.getStage().getCode());
            DbUtils.setLongZeroToNull(pstmt, ++i, shuffling.getAssigneeAccountId());
            DbUtils.setArrayEmptyToNull(pstmt, ++i, shuffling.getRecipientPublicKeys());
            pstmt.setByte(++i, shuffling.getRegistrantCount());
            pstmt.setInt(++i, shuffling.getHeight());
            pstmt.executeUpdate();
            log.trace("Save shuffling {} - height - {} remaining - {} Trace - {}", shuffling.getId(), shuffling.getHeight(), shuffling.getBlocksRemaining(), shuffling.last3Stacktrace());
        }
    }
    public List<Shuffling> getAccountShufflings(long accountId, boolean includeFinished, int from, int to) {
        Connection con = null;
        try {
            con = getDatabaseManager().getDataSource().getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT shuffling.* FROM shuffling, shuffling_participant WHERE "
                    + "shuffling_participant.account_id = ? AND shuffling.id = shuffling_participant.shuffling_id "
                    + (includeFinished ? "" : "AND shuffling.blocks_remaining IS NOT NULL ")
                    + "AND shuffling.latest = TRUE AND shuffling_participant.latest = TRUE ORDER BY blocks_remaining NULLS LAST, height DESC "
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return CollectionUtil.toList(getManyBy(con, pstmt, false));
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
}
