/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.shuffling.mapper.ShufflingMapper;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.core.shuffling.service.Stage;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
public class ShufflingTable extends VersionedDeletableEntityDbTable<Shuffling> implements ShufflingRepository{
    private static final String TABLE_NAME = "shuffling";
    private ShufflingMapper mapper;
    private ShufflingKeyFactory keyFactory;
    @Inject
    public ShufflingTable(ShufflingKeyFactory keyFactory, ShufflingMapper mapper) {
        super(TABLE_NAME, keyFactory, false);
        this.mapper = mapper;
        this.keyFactory = keyFactory;
    }

    @Override
    public Shuffling load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return mapper.map(rs, null);
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

    @Override
    public Shuffling get(long id) {
        return get(keyFactory.newKey(id));
    }

    @Override
    public int getActiveCount() {
        return getCount(new DbClause.NotNullClause("blocks_remaining"));
    }

    @Override
    public List<Shuffling> extractAll(int from, int to) {
        return CollectionUtil.toList(getAll(from, to, " ORDER BY blocks_remaining NULLS LAST, height DESC "));
    }

    @Override
    public List<Shuffling> getActiveShufflings(int from, int to) {
        return CollectionUtil.toList(getManyBy(new DbClause.NotNullClause("blocks_remaining"), from, to, " ORDER BY blocks_remaining, height DESC "));
    }

    @Override
    public List<Shuffling> getFinishedShufflings(int from, int to) {
        return CollectionUtil.toList(getManyBy(new DbClause.NullClause("blocks_remaining"), from, to, " ORDER BY height DESC "));
    }

    @Override
    public int getHoldingShufflingCount(long holdingId, boolean includeFinished) {
        DbClause clause = holdingId != 0 ? new DbClause.LongClause("holding_id", holdingId) : new DbClause.NullClause("holding_id");
        if (!includeFinished) {
            clause = clause.and(new DbClause.NotNullClause("blocks_remaining"));
        }
        return getCount(clause);
    }

    @Override
    public List<Shuffling> getHoldingShufflings(long holdingId, Stage stage, boolean includeFinished, int from, int to) {
        DbClause clause = holdingId != 0 ? new DbClause.LongClause("holding_id", holdingId) : new DbClause.NullClause("holding_id");
        if (!includeFinished) {
            clause = clause.and(new DbClause.NotNullClause("blocks_remaining"));
        }
        if (stage != null) {
            clause = clause.and(new DbClause.ByteClause("stage", stage.getCode()));
        }
        return CollectionUtil.toList(getManyBy(clause, from, to, " ORDER BY blocks_remaining NULLS LAST, height DESC "));
    }

    @Override
    public List<Shuffling> getAssignedShufflings(long assigneeAccountId, int from, int to) {
        return CollectionUtil.toList(getManyBy(new DbClause.LongClause("assignee_account_id", assigneeAccountId)
                        .and(new DbClause.ByteClause("stage", Stage.PROCESSING.getCode())), from, to,
                " ORDER BY blocks_remaining NULLS LAST, height DESC "));
    }
}
