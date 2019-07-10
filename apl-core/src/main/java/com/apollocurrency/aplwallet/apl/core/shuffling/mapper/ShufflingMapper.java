/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.mapper;

import com.apollocurrency.aplwallet.apl.core.shuffling.service.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ShufflingMapper extends VersionedDerivedEntityMapper<Shuffling> {


    public ShufflingMapper(KeyFactory<Shuffling> keyFactory) {
        super(keyFactory);
    }

    @Override
    public Shuffling doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long id = rs.getLong("id");
        long holdingId = rs.getLong("holding_id");
        HoldingType holdingType = HoldingType.get(rs.getByte("holding_type"));
        long issuerId = rs.getLong("issuer_id");
        long amount = rs.getLong("amount");
        byte participantCount = rs.getByte("participant_count");
        short blocksRemaining = rs.getShort("blocks_remaining");
        ShufflingService.Stage stage = ShufflingService.Stage.get(rs.getByte("stage"));
        long assigneeAccountId = rs.getLong("assignee_account_id");
        byte[][] recipientPublicKeys = DbUtils.getArray(rs, "recipient_public_keys", byte[][].class, Convert.EMPTY_BYTES);
        byte registrantCount = rs.getByte("registrant_count");
        return new Shuffling(null, id, holdingId, holdingType, issuerId, amount, participantCount, blocksRemaining, registrantCount, stage, assigneeAccountId, recipientPublicKeys, null);
    }
}
