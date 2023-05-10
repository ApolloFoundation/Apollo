package com.apollocurrency.aplwallet.apl.core.dao.appdata.impl;

import com.apollocurrency.aplwallet.apl.core.converter.db.ReferencedTransactionRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class ReferencedTransactionDaoImpl extends EntityDbTable<ReferencedTransaction> implements ReferencedTransactionDao {
    private static final LongKeyFactory<ReferencedTransaction> KEY_FACTORY = new LongKeyFactory<ReferencedTransaction>("transaction_id") {
        @Override
        public DbKey newKey(ReferencedTransaction referencedTransaction) {
            return new LongKey(referencedTransaction.getTransactionId());
        }
    };
    private static final String TABLE = "referenced_transaction";
    private static final ReferencedTransactionRowMapper REFERENCED_ROW_MAPPER = new ReferencedTransactionRowMapper();
    private final TransactionEntityRowMapper transactionRowMapper;

    @Inject
    public ReferencedTransactionDaoImpl(DatabaseManager databaseManager,
                                        TransactionEntityRowMapper transactionRowMapper,
                                        Event<FullTextOperationData> fullTextOperationDataEvent) {
        super(TABLE, KEY_FACTORY, false, null,
            databaseManager, fullTextOperationDataEvent);
        this.transactionRowMapper = transactionRowMapper;
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
        return Optional.ofNullable(get(KEY_FACTORY.newKey(transactionId)))
            .flatMap(e -> Optional.of(e.getReferencedTransactionId()));
    }

    @Override
    public List<Long> getAllReferencedTransactionIds() {
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstm = con.prepareStatement("SELECT referenced_transaction_id FROM referenced_transaction")) {

            List<Long> referencedTransactionIds = new ArrayList<>();
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    referencedTransactionIds.add(rs.getLong(1));
                }
            }
            return referencedTransactionIds;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int save(ReferencedTransaction referencedTransaction) {
        try (Connection con = databaseManager.getDataSource().getConnection()) {
            save(con, referencedTransaction);
            return 1;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<TransactionEntity> getReferencingTransactions(long transactionId, int from, Integer limit) {
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstm = con.prepareStatement("SELECT transaction.* FROM transaction, referenced_transaction "
                 + "WHERE referenced_transaction.referenced_transaction_id = ? "
                 + "AND referenced_transaction.transaction_id = transaction.id "
                 + "ORDER BY transaction.block_timestamp DESC, transaction.transaction_index DESC "
                 + "LIMIT ? OFFSET ?")) {
            pstm.setLong(1, transactionId);
            pstm.setInt(2, limit);
            pstm.setInt(3, from);
            List<TransactionEntity> referencingTransactions = new ArrayList<>();
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    referencingTransactions.add(transactionRowMapper.map(rs, null));
                }
            }
            return referencingTransactions;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
