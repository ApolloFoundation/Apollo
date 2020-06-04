package com.apollocurrency.aplwallet.apl.core.db.dao;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.converter.db.ReferencedTransactionRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionRowMapper;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import org.jdbi.v3.core.Jdbi;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Singleton
public class ReferencedTransactionDaoImpl extends EntityDbTable<ReferencedTransaction> implements ReferencedTransactionDao {
    private static final KeyFactory<ReferencedTransaction> KEY_FACTORY = new LongKeyFactory<ReferencedTransaction>("transaction_id") {
        @Override
        public DbKey newKey(ReferencedTransaction referencedTransaction) {
            return new LongKey(referencedTransaction.getTransactionId());
        }
    };
    private static final String TABLE = "referenced_transaction";
    private static final ReferencedTransactionRowMapper REFERENCED_ROW_MAPPER = new ReferencedTransactionRowMapper();
    private static final TransactionRowMapper TRANSACTION_ROW_MAPPER = new TransactionRowMapper();

    public ReferencedTransactionDaoImpl() {
        super(TABLE, KEY_FACTORY, false);
    }

    @Override
    public ReferencedTransaction load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return REFERENCED_ROW_MAPPER.map(rs, null);
    }

    @Override
    public void save(Connection con, ReferencedTransaction referencedTransaction) throws SQLException {
        try (PreparedStatement pstm = con.prepareStatement("INSERT INTO referenced_transaction (transaction_id, referenced_transaction_id, height) VALUES (?, ?, ?)")) {
            pstm.setLong(1, referencedTransaction.getTransactionId());
            pstm.setLong(2, referencedTransaction.getReferencedTransactionId());
            pstm.setInt(3, referencedTransaction.getHeight());
            pstm.executeUpdate();
        }
    }

    @Override
    public Optional<Long> getReferencedTransactionIdFor(long transactionId) {
        Jdbi jdbi = getDatabaseManager().getJdbi();
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT referenced_transaction_id FROM referenced_transaction where transaction_id = :transactionId")
                .bind("transactionId", transactionId)
                .mapTo(Long.class)
                .findFirst()
        );
    }

    @Override
    public List<Long> getAllReferencedTransactionIds() {
        Jdbi jdbi = getDatabaseManager().getJdbi();
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT referenced_transaction_id FROM referenced_transaction")
                .mapTo(Long.class)
                .list()
        );
    }

    @Override
    public int save(ReferencedTransaction referencedTransaction) {
        Jdbi jdbi = getDatabaseManager().getJdbi();
        return jdbi.withHandle(handle ->
            handle.inTransaction(h ->
                h.createUpdate("INSERT INTO referenced_transaction (transaction_id, referenced_transaction_id, height) VALUES (:transactionId, :referencedTransactionId, :height)")
                    .bindBean(referencedTransaction)
                    .execute()
            ));
    }

    @Override
    public List<Transaction> getReferencingTransactions(long transactionId, int from, Integer limit) {
        Jdbi jdbi = getDatabaseManager().getJdbi();
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT transaction.* FROM transaction, referenced_transaction "
                + "WHERE referenced_transaction.referenced_transaction_id = :transactionId "
                + "AND referenced_transaction.transaction_id = transaction.id "
                + "ORDER BY transaction.block_timestamp DESC, transaction.transaction_index DESC "
                + "OFFSET :from FETCH FIRST :limit ROWS ONLY")
                .bind("transactionId", transactionId)
                .bind("from", from)
                .bind("limit", limit)
                .map(TRANSACTION_ROW_MAPPER)
                .list()
        );
    }
}
