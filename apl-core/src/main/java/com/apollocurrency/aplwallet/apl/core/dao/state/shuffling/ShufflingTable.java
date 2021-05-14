/*
 * Copyright © 2020-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Singleton
public class ShufflingTable extends VersionedDeletableEntityDbTable<Shuffling> implements ShufflingRepository {

    public static final LongKeyFactory<Shuffling> dbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(Shuffling shuffling) {
            if (shuffling.getDbKey() == null) {
                shuffling.setDbKey(dbKeyFactory.newKey(shuffling.getId()));
            }
            return shuffling.getDbKey();
        }
    };

    @Inject
    public ShufflingTable(DatabaseManager databaseManager,
                          Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("shuffling", dbKeyFactory, null,
                databaseManager, deleteOnTrimDataEvent);
    }

    @Override
    public void save(Connection con, Shuffling shuffling) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.SET_ARRAY)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO shuffling (id, holding_id, holding_type, "
                + "issuer_id, amount, participant_count, blocks_remaining, stage, assignee_account_id, "
                + "recipient_public_keys, registrant_count, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE) "
                + "ON DUPLICATE KEY UPDATE id = VALUES(id), holding_id = VALUES(holding_id), holding_type = VALUES(holding_type), "
                + "issuer_id = VALUES(issuer_id), amount = VALUES(amount), participant_count = VALUES(participant_count), "
                + "blocks_remaining = VALUES(blocks_remaining), stage = VALUES(stage), assignee_account_id = VALUES(assignee_account_id), "
                + "recipient_public_keys = VALUES(recipient_public_keys), registrant_count = VALUES(registrant_count), "
                + "height = VALUES(height), latest = TRUE, deleted = FALSE")
        ) {
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
            pstmt.executeUpdate();
        }

        log.trace("Save shuffling {} - height - {} remaining - {} Trace - {}",
            shuffling.getId(), shuffling.getHeight(), shuffling.getBlocksRemaining(), ThreadUtils.last3Stacktrace());
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

}
