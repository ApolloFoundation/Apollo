/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.operation.phasing;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.converter.phasing.PhasingPollLinkedTransactionMapper;
import com.apollocurrency.aplwallet.apl.core.entity.operation.phasing.PhasingPollLinkedTransaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private final Blockchain blockchain;

    @Inject
    public PhasingPollLinkedTransactionTable(Blockchain blockchain) {
        super(TABLE_NAME, false, KEY_FACTORY, false);
        this.blockchain = Objects.requireNonNull(blockchain, "Blockchain is NULL");
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

    public List<Transaction> getLinkedPhasedTransactions(byte[] linkedTransactionFullHash) throws SQLException {
        try (Connection con = getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT transaction_id FROM phasing_poll_linked_transaction " +
                 "WHERE linked_transaction_id = ? AND linked_full_hash = ?")) {
            int i = 0;
            pstmt.setLong(++i, Convert.fullHashToId(linkedTransactionFullHash));
            pstmt.setBytes(++i, linkedTransactionFullHash);
            List<Transaction> transactions = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(blockchain.getTransaction(rs.getLong("transaction_id")));
                }
            }
            return transactions;
        }
    }
}
