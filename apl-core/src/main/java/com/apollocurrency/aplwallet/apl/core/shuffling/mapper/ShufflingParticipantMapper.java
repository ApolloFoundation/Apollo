/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.mapper;

import com.apollocurrency.aplwallet.apl.core.shuffling.service.ShufflingParticipantService;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ShufflingParticipantMapper extends VersionedDerivedEntityMapper<ShufflingParticipant> {
    public ShufflingParticipantMapper(KeyFactory<ShufflingParticipant> keyFactory) {
        super(keyFactory);
    }

    @Override
    public ShufflingParticipant doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long shufflingId = rs.getLong("shuffling_id");
        long accountId = rs.getLong("account_id");
        long nextAccountId = rs.getLong("next_account_id");
        int index = rs.getInt("participant_index");
        ShufflingParticipantService.State state = ShufflingParticipantService.State.get(rs.getByte("state"));
        byte[][] blameData = DbUtils.getArray(rs, "blame_data", byte[][].class, Convert.EMPTY_BYTES);
        byte[][] keySeeds = DbUtils.getArray(rs, "key_seeds", byte[][].class, Convert.EMPTY_BYTES);
        byte[] dataTransactionFullHash = rs.getBytes("data_transaction_full_hash");
        byte[] dataHash = rs.getBytes("data_hash");
        return new ShufflingParticipant(null, null, shufflingId, accountId, index, nextAccountId, state, blameData, keySeeds, dataTransactionFullHash, dataHash);
    }
}
