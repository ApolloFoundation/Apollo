/*
 * Copyright © 2020-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Vetoed;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Slf4j
@Vetoed
public class ShufflingTable extends VersionedDeletableEntityDbTable<Shuffling> implements ShufflingRepository {

    public ShufflingTable(DatabaseManager databaseManager,
                          Event<FullTextOperationData> fullTextOperationDataEvent) {
        super("shuffling", dbKeyFactory, null,
                databaseManager, fullTextOperationDataEvent);
    }

    @Override
    public void save(Connection con, Shuffling shuffling) throws SQLException {
        if (shuffling.requireMerge()) {
            doUpdate(con, shuffling);
        } else {
            doInsert(con, shuffling);
        }
    }

    @Override
    protected Shuffling load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return  new Shuffling(rs, dbKey);
    }


    @Override
    public int getCount() {
        return super.getCount();
    }

    public int getActiveCount() {
        return getCount(new DbClause.NotNullClause("blocks_remaining"));
    }

    @Override
    public List<Shuffling> extractAll(int from, int to) {
        return CollectionUtil.toList(getAll(from, to));
    }

    public DbIterator<Shuffling> getAll(int from, int to) {
        return getAll(from, to, " ORDER BY -blocks_remaining DESC, height DESC ");
    }

    @Override
    public List<Shuffling> getActiveShufflings(int from, int to) {
        return CollectionUtil.toList(getManyBy(new DbClause.NotNullClause("blocks_remaining"), from, to, " ORDER BY blocks_remaining, height DESC "));
    }

    public List<Shuffling> getActiveShufflings() {
        try (
            Connection con = databaseManager.getDataSource().getConnection();
            PreparedStatement psActiveShuffling = con.prepareStatement("SELECT * FROM shuffling WHERE blocks_remaining IS NOT NULL AND latest = TRUE ORDER BY blocks_remaining, height DESC");
        ){
            return CollectionUtil.toList(getManyBy(con, psActiveShuffling, true));
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Shuffling> getFinishedShufflings(int from, int to) {
        return CollectionUtil.toList(getManyBy(new DbClause.NullClause("blocks_remaining"), from, to, " ORDER BY height DESC "));
    }

    @Override
    public Shuffling get(long shufflingId) {
        return getShuffling(shufflingId);
    }

    public Shuffling getShuffling(long shufflingId) {
        return get(dbKeyFactory.newKey(shufflingId));
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
    public List<Shuffling> getHoldingShufflings(long holdingId, ShufflingStage stage, boolean includeFinished, int from, int to) {
        DbClause clause = holdingId != 0 ? new DbClause.LongClause("holding_id", holdingId) : new DbClause.NullClause("holding_id");
        if (!includeFinished) {
            clause = clause.and(new DbClause.NotNullClause("blocks_remaining"));
        }
        if (stage != null) {
            clause = clause.and(new DbClause.ByteClause("stage", stage.getCode()));
        }
        return CollectionUtil.toList(getManyBy(clause, from, to, " ORDER BY -blocks_remaining DESC, height DESC "));
    }

    public List<Shuffling> getAccountShufflings(long accountId, boolean includeFinished, int from, int to) {
        Connection con = null;
        try {
            con = databaseManager.getDataSource().getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT shuffling.* FROM shuffling, shuffling_participant WHERE "
                + "shuffling_participant.account_id = ? AND shuffling.id = shuffling_participant.shuffling_id "
                + (includeFinished ? "" : "AND shuffling.blocks_remaining IS NOT NULL ")
                + "AND shuffling.latest = TRUE AND shuffling_participant.latest = TRUE ORDER BY -blocks_remaining DESC, height DESC "
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
    public List<Shuffling> getAssignedShufflings(long assigneeAccountId, int from, int to) {
        return CollectionUtil.toList(getManyBy(new DbClause.LongClause("assignee_account_id", assigneeAccountId)
                .and(new DbClause.ByteClause("stage", ShufflingStage.PROCESSING.getCode())), from, to,
            " ORDER BY -blocks_remaining DESC, height DESC "));
    }

    @Override
    public boolean delete(Shuffling shuffling) {
        return deleteAtHeight(shuffling, shuffling.getHeight());
    }

    private void doInsert(Connection con, Shuffling shuffling) throws SQLException {
        try (
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO shuffling (id, holding_id, holding_type, "
                + "issuer_id, amount, participant_count, blocks_remaining, stage, assignee_account_id, "
                + "recipient_public_keys, registrant_count, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE) ", Statement.RETURN_GENERATED_KEYS)
        ) {
            setPstmParams(pstmt, shuffling);
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    shuffling.setDbId(generatedKeys.getLong(1));
                } else {
                    throw new IllegalStateException("Unable to retrieve generated primary key for the shuffling " + shuffling);
                }
            };
        }
        log.trace("Insert shuffling {} - height - {} remaining - {} Trace - {}",
            shuffling.getId(), shuffling.getHeight(), shuffling.getBlocksRemaining(), ThreadUtils.last3Stacktrace());
    }

    private void doUpdate(Connection con, Shuffling shuffling) throws SQLException {
        try (
            PreparedStatement pstmt = con.prepareStatement("UPDATE shuffling SET id = ?, " +
                " holding_id = ?, holding_type = ?, "
                + "issuer_id = ?, amount = ?, participant_count = ?, "
                + "blocks_remaining = ?, stage = ?, assignee_account_id = ?, "
                + "recipient_public_keys = ?, registrant_count = ?, "
                + "height = ?, latest = TRUE, deleted = FALSE WHERE db_id = ?")
        ) {
            int index = setPstmParams(pstmt, shuffling);
            pstmt.setLong(++index, shuffling.getDbId());
            pstmt.executeUpdate();
        }

        log.trace("Merge shuffling {} - height - {} remaining - {} Trace - {}",
            shuffling.getId(), shuffling.getHeight(), shuffling.getBlocksRemaining(), ThreadUtils.last3Stacktrace());
    }

    private int setPstmParams(PreparedStatement pstmt, Shuffling shuffling) throws SQLException {
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
        DbUtils.set2dByteArray(pstmt, ++i, shuffling.getRecipientPublicKeys());
        pstmt.setByte(++i, shuffling.getRegistrantCount());
        pstmt.setInt(++i, shuffling.getHeight());
        return i;
    }

}
