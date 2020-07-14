/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db.phasing;

import com.apollocurrency.aplwallet.apl.core.converter.db.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollLinkedTransaction;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingPollLinkedTransactionMapper extends DerivedEntityMapper<PhasingPollLinkedTransaction> {
    public PhasingPollLinkedTransactionMapper(KeyFactory<PhasingPollLinkedTransaction> keyFactory) {
        super(keyFactory);
    }

    @Override
    public PhasingPollLinkedTransaction doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long pollId = rs.getLong("transaction_id");
        long linkedTransactionId = rs.getLong("linked_transaction_id");
        byte[] fullHash = rs.getBytes("linked_full_hash");
        return new PhasingPollLinkedTransaction(null, null, pollId, linkedTransactionId, fullHash);
    }
}
