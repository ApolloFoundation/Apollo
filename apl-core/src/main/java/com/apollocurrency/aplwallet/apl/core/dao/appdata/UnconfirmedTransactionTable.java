/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionSerializer;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class UnconfirmedTransactionTable extends EntityDbTable<UnconfirmedTransaction> {

    private final LongKeyFactory<UnconfirmedTransaction> transactionKeyFactory;
    private final TransactionBuilder transactionBuilder;
    private final TransactionSerializer transactionSerializer;
    private final IteratorToStreamConverter<UnconfirmedTransaction> streamConverter;

    @Inject
    public UnconfirmedTransactionTable(LongKeyFactory<UnconfirmedTransaction> transactionKeyFactory,
                                       TransactionBuilder transactionBuilder,
                                       TransactionSerializer transactionSerializer,
                                       DerivedTablesRegistry derivedDbTablesRegistry,
                                       DatabaseManager databaseManager) {
        super("unconfirmed_transaction", transactionKeyFactory, false, null,
            derivedDbTablesRegistry, databaseManager, null);
        this.transactionKeyFactory = transactionKeyFactory;
        this.transactionBuilder = transactionBuilder;
        this.transactionSerializer = transactionSerializer;
        this.streamConverter = new IteratorToStreamConverter<>();
    }

    @Override
    public UnconfirmedTransaction load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        try {
            byte[] transactionBytes = rs.getBytes("transaction_bytes");
            JSONObject prunableAttachments = null;
            String prunableJSON = rs.getString("prunable_json");
            if (prunableJSON != null) {
                prunableAttachments = (JSONObject) JSONValue.parse(prunableJSON);
            }
            Transaction tx = transactionBuilder.newTransactionBuilder(transactionBytes, prunableAttachments).build();
            tx.setHeight(rs.getInt("transaction_height"));
            long arrivalTimestamp = rs.getLong("arrival_timestamp");
            long feePerByte = rs.getLong("fee_per_byte");
            return new UnconfirmedTransaction(tx, arrivalTimestamp, feePerByte);
        } catch (AplException.ValidationException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void save(Connection con, UnconfirmedTransaction unconfirmedTransaction) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO unconfirmed_transaction (id, transaction_height, "
            + "fee_per_byte, expiration, transaction_bytes, prunable_json, arrival_timestamp, height) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, unconfirmedTransaction.getId());
            pstmt.setInt(++i, unconfirmedTransaction.getHeight());
            pstmt.setLong(++i, unconfirmedTransaction.getFeePerByte());
            pstmt.setInt(++i, unconfirmedTransaction.getExpiration());
            pstmt.setBytes(++i, unconfirmedTransaction.getCopyTxBytes());
            JSONObject prunableJSON = transactionSerializer.getPrunableAttachmentJSON(unconfirmedTransaction);
            if (prunableJSON != null) {
                pstmt.setString(++i, prunableJSON.toJSONString());
            } else {
                pstmt.setNull(++i, Types.VARCHAR);
            }
            pstmt.setLong(++i, unconfirmedTransaction.getArrivalTimestamp());
            pstmt.setInt(++i, unconfirmedTransaction.getHeight());
            pstmt.executeUpdate();
        }
    }

    public int deleteById(long id) {
        TransactionalDataSource dataSource = getDatabaseManager().getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM unconfirmed_transaction WHERE id = ?")) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            databaseManager.getDataSource().rollback();
            throw new RuntimeException(e.toString(), e);
        }
    }

        @Override
    public String defaultSort() {
        return " ORDER BY transaction_height ASC, fee_per_byte DESC, arrival_timestamp ASC, id ASC ";
    }

    public Stream<UnconfirmedTransaction> getExpiredTxsStream(int time) {
        return streamConverter.apply(getManyBy(new DbClause.IntClause("expiration", DbClause.Op.LT, time), 0, -1, ""));
    }

    public int countExpiredTransactions(int epochTime) {
        return super.getCount(
            new DbClause.IntClause("expiration", DbClause.Op.LT, epochTime));
    }


    public Stream<UnconfirmedTransaction> getAllUnconfirmedTransactions() {
        return streamConverter.convert(this.getAll(0, -1));
    }

    public List<Long> getAllUnconfirmedTransactionIds() {
        List<Long> result = new ArrayList<>();
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM unconfirmed_transaction");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getLong("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }

    public LongKeyFactory<UnconfirmedTransaction> getTransactionKeyFactory() {
        return transactionKeyFactory;
    }



}
