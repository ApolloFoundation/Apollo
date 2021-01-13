/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipantState;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class ShufflingParticipantTable  extends VersionedDeletableEntityDbTable<ShufflingParticipant> {

    public static final LinkKeyFactory<ShufflingParticipant> dbKeyFactory = new LinkKeyFactory<>("shuffling_id", "account_id") {
        @Override
        public DbKey newKey(ShufflingParticipant participant) {
            return participant.getDbKey();
        }

    };

    @Inject
    public ShufflingParticipantTable(DerivedTablesRegistry derivedDbTablesRegistry,
                                     DatabaseManager databaseManager,
                                     Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("shuffling_participant", dbKeyFactory, null,
            derivedDbTablesRegistry, databaseManager, null, deleteOnTrimDataEvent);
    }

    @Override
    public ShufflingParticipant load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new ShufflingParticipant(rs, dbKey);
    }

    @Override
    public void save(Connection con, ShufflingParticipant participant) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO shuffling_participant (shuffling_id, "
                + "account_id, next_account_id, participant_index, state, blame_data, key_seeds, data_transaction_full_hash, data_hash, height, latest, deleted) "
                + "KEY (shuffling_id, account_id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, participant.getShufflingId());
            pstmt.setLong(++i, participant.getAccountId());
            DbUtils.setLongZeroToNull(pstmt, ++i, participant.getNextAccountId());
            pstmt.setInt(++i, participant.getIndex());
            pstmt.setByte(++i, participant.getState().getCode());
            DbUtils.setArrayEmptyToNull(pstmt, ++i, participant.getBlameData());
            DbUtils.setArrayEmptyToNull(pstmt, ++i, participant.getKeySeeds());
            DbUtils.setBytes(pstmt, ++i, participant.getDataTransactionFullHash());
            DbUtils.setBytes(pstmt, ++i, participant.getDataHash());
            pstmt.setInt(++i, participant.getHeight());
            pstmt.executeUpdate();
        }
    }

    public DbIterator<ShufflingParticipant> getParticipants(long shufflingId) {
        return getManyBy(new DbClause.LongClause("shuffling_id", shufflingId), 0, -1, " ORDER BY participant_index ");
    }

    public ShufflingParticipant getParticipant(long shufflingId, long accountId) {
        return get(dbKeyFactory.newKey(shufflingId, accountId));
    }

    public ShufflingParticipant getLastParticipant(long shufflingId) {
        return getBy(new DbClause.LongClause("shuffling_id", shufflingId).and(new DbClause.NullClause("next_account_id")));
    }

    public void addParticipant(ShufflingParticipant participant) {
        insert(participant);
    }

    public int getVerifiedCount(long shufflingId) {
        return getCount(new DbClause.LongClause("shuffling_id", shufflingId).and(
            new DbClause.ByteClause("state", ShufflingParticipantState.VERIFIED.getCode())));
    }

    public ShufflingParticipant getPreviousParticipant(ShufflingParticipant participant) {
        return getBy(new DbClause.LongClause("shuffling_id", participant.getShufflingId()).and(new DbClause.IntClause("participant_index", participant.getIndex() - 1)));
    }

}
