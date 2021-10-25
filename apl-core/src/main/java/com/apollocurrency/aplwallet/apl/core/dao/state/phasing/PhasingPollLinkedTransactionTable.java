/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.phasing;

import com.apollocurrency.aplwallet.apl.core.converter.db.phasing.PhasingPollLinkedTransactionMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollLinkedTransaction;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class PhasingPollLinkedTransactionTable extends ValuesDbTable<PhasingPollLinkedTransaction> {
    public static final String TABLE_NAME = "phasing_poll_linked_transaction";
    private static final LongKeyFactory<PhasingPollLinkedTransaction> KEY_FACTORY = new LongKeyFactory<PhasingPollLinkedTransaction>("transaction_id") {
        @Override
        public DbKey newKey(PhasingPollLinkedTransaction poll) {
            if (poll.getDbKey() == null) {
                poll.setDbKey(new LongKey(poll.getPollId()));
            }
            return poll.getDbKey();
        }
    };
    private static final PhasingPollLinkedTransactionMapper MAPPER = new PhasingPollLinkedTransactionMapper(KEY_FACTORY);

    @Inject
    public PhasingPollLinkedTransactionTable(DatabaseManager databaseManager,
                                             Event<FullTextOperationData> fullTextOperationDataEvent) {
        super(TABLE_NAME, KEY_FACTORY, false, databaseManager, fullTextOperationDataEvent);
    }

    @Override
    public PhasingPollLinkedTransaction load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return MAPPER.map(rs, null);
    }

    public List<PhasingPollLinkedTransaction> get(long id) {
        return get(KEY_FACTORY.newKey(id));
    }

    @Override
    public void save(Connection con, PhasingPollLinkedTransaction linkedTransaction) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_poll_linked_transaction (transaction_id, "
            + "linked_full_hash, linked_transaction_id, height) VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, linkedTransaction.getPollId());
            pstmt.setBytes(++i, linkedTransaction.getFullHash());
            pstmt.setLong(++i, linkedTransaction.getTransactionId());
            pstmt.setInt(++i, linkedTransaction.getHeight());
            pstmt.executeUpdate();
        }
    }

    public List<Long> getLinkedPhasedTransactionIds(byte[] linkedTransactionFullHash) throws SQLException {
        try (Connection con = getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT transaction_id FROM phasing_poll_linked_transaction " +
                 "WHERE linked_transaction_id = ? AND linked_full_hash = ?")) {
            int i = 0;
            pstmt.setLong(++i, Convert.transactionFullHashToId(linkedTransactionFullHash));
            pstmt.setBytes(++i, linkedTransactionFullHash);
            List<Long> transactions = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(rs.getLong("transaction_id"));
                }
            }
            return transactions;
        }
    }
}
