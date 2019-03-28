/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.dao;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PhasingPollLinkedTransactionTable extends ValuesDbTable<PhasingPoll, byte[]> {
    public static final String TABLE_NAME = "phasing_poll_linked_transaction";
    private static final LongKeyFactory<PhasingPoll> KEY_FACTORY = new LongKeyFactory<PhasingPoll>("transaction_id") {
        @Override
        public DbKey newKey(PhasingPoll poll) {
            return new LongKey(poll.getId());
        }
    };
    private final Blockchain blockchain;

    @Inject
    public PhasingPollLinkedTransactionTable(Blockchain blockchain) {
        super(TABLE_NAME, KEY_FACTORY);
        this.blockchain = Objects.requireNonNull(blockchain, "Blockchain is NULL");
    }

    @Override
    protected byte[] load(Connection con, ResultSet rs) throws SQLException {
        return rs.getBytes("linked_full_hash");
    }

    public List<byte[]> get(long id) {
        return get(KEY_FACTORY.newKey(id));
    }

    @Override
    protected void save(Connection con, PhasingPoll poll, byte[] linkedFullHash) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_poll_linked_transaction (transaction_id, "
                + "linked_full_hash, linked_transaction_id, height) VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, poll.getId());
            pstmt.setBytes(++i, linkedFullHash);
            pstmt.setLong(++i, Convert.fullHashToId(linkedFullHash));
            pstmt.setInt(++i, blockchain.getHeight());
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
