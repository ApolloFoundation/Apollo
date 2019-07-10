/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.LinkKey;
import com.apollocurrency.aplwallet.apl.core.db.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.shuffling.mapper.ShufflingMapper;
import com.apollocurrency.aplwallet.apl.core.shuffling.mapper.ShufflingParticipantMapper;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingParticipant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.inject.Singleton;

@Singleton
public class ShufflingParticipantTable extends VersionedDeletableEntityDbTable<ShufflingParticipant> {
    public static final String TABLE_NAME = "shuffling_participant";
    private static final LinkKeyFactory<ShufflingParticipant> KEY_FACTORY = new LinkKeyFactory<>("shuffling_id", "account_id") {

        @Override
        public DbKey newKey(ShufflingParticipant participant) {
            if (participant.getDbKey() == null) {
                participant.setDbKey(new LinkKey(participant.getShufflingId(), participant.getAccountId()));
            }
            return participant.getDbKey();
        }
    };
    private static final ShufflingParticipantMapper MAPPER = new ShufflingParticipantMapper(KEY_FACTORY);


    public ShufflingParticipantTable() {
        super(TABLE_NAME, KEY_FACTORY, false);
    }


    @Override
    public void save(Connection con, ShufflingParticipant entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO shuffling_participant (shuffling_id, "
                + "account_id, next_account_id, participant_index, state, blame_data, key_seeds, data_transaction_full_hash, data_hash, height, latest) "
                + "KEY (shuffling_id, account_id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, entity.getShufflingId());
            pstmt.setLong(++i, entity.getAccountId());
            DbUtils.setLongZeroToNull(pstmt, ++i, entity.getNextAccountId());
            pstmt.setInt(++i, entity.getIndex());
            pstmt.setByte(++i, entity.getState().getCode());
            DbUtils.setArrayEmptyToNull(pstmt, ++i, entity.getBlameData());
            DbUtils.setArrayEmptyToNull(pstmt, ++i, entity.getKeySeeds());
            DbUtils.setBytes(pstmt, ++i, entity.getDataTransactionFullHash());
            DbUtils.setBytes(pstmt, ++i, entity.getDataHash());
            pstmt.setInt(++i, entity.getHeight());
            pstmt.executeUpdate();
        }
    }

    public ShufflingParticipant get(long shufflingId, long accountId) {
        return get(KEY_FACTORY.newKey(shufflingId, accountId));
    }

    @Override
    protected ShufflingParticipant load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return MAPPER.map(rs, null);
    }
}
