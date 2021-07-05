/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.converter.db.ExchangeContractMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.dex.ExchangeContract;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Use DexContractDao for not transactional operations. ( f.e. search)
 * DEX Contract in derived table hierarchy is used for exporting/importing shard data.
 */
@Slf4j
@Singleton
public class DexContractTable extends EntityDbTable<ExchangeContract> {

    static final LongKeyFactory<ExchangeContract> KEY_FACTORY = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(ExchangeContract exchangeContract) {
            return new LongKey(exchangeContract.getId());
        }
    };

    private static final String TABLE_NAME = "dex_contract";
    private ExchangeContractMapper mapper = new ExchangeContractMapper();

    @Inject
    public DexContractTable(DatabaseManager databaseManager,
                            Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super(TABLE_NAME, KEY_FACTORY, true, null, databaseManager, deleteOnTrimDataEvent);
    }

    private static ExchangeContract getFirstOrNull(List<ExchangeContract> contracts) {
        if (contracts.size() > 0) {
            return contracts.get(0);
        } else {
            return null;
        }
    }

    @Override
    protected ExchangeContract load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return mapper.map(rs, null);
    }

    @Override
    public void save(Connection con, ExchangeContract entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO dex_contract (id, offer_id, counter_offer_id, " +
            "sender, recipient, secret_hash, encrypted_secret, transfer_tx_id, counter_transfer_tx_id, deadline_to_reply, status, height, latest) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE) "
            + "ON DUPLICATE KEY UPDATE "
            + "id = VALUES(id), offer_id = VALUES(offer_id), counter_offer_id = VALUES(counter_offer_id), "
            + "sender = VALUES(sender), recipient = VALUES(recipient), secret_hash = VALUES(secret_hash), "
            + "encrypted_secret = VALUES(encrypted_secret), transfer_tx_id = VALUES(transfer_tx_id), "
            + "counter_transfer_tx_id = VALUES(counter_transfer_tx_id), deadline_to_reply = VALUES(deadline_to_reply), "
            + "status = VALUES(status), height = VALUES(height), latest = TRUE")
        ) {
            int i = 0;
            pstmt.setLong(++i, entity.getId());
            pstmt.setLong(++i, entity.getOrderId());
            pstmt.setLong(++i, entity.getCounterOrderId());
            pstmt.setLong(++i, entity.getSender());
            pstmt.setLong(++i, entity.getRecipient());
            pstmt.setBytes(++i, entity.getSecretHash());
            pstmt.setBytes(++i, entity.getEncryptedSecret());
            pstmt.setString(++i, entity.getTransferTxId());
            pstmt.setString(++i, entity.getCounterTransferTxId());
            pstmt.setInt(++i, entity.getDeadlineToReply());
            pstmt.setByte(++i, (byte) entity.getContractStatus().ordinal());
            pstmt.setInt(++i, entity.getHeight());

            pstmt.executeUpdate();
        }
    }

    public ExchangeContract getById(Long id) {
        return get(KEY_FACTORY.newKey(id));
    }

    public List<ExchangeContract> getAllByCounterOrder(Long counterOrderId) {
        return getAllByLongParameterFromStatus(counterOrderId, "counter_offer_id", 0);
    }

    private List<ExchangeContract> getAllByLongParameterFromStatusHeightSorted(Long parameterValue, String parameterName, int fromStatus) {
        DbIterator<ExchangeContract> dbIterator = getManyBy(new DbClause.LongClause(parameterName, parameterValue).and(new DbClause.ByteClause("status", DbClause.Op.GTE, (byte) fromStatus)), 0, -1, " ORDER BY height DESC, db_id DESC");
        return CollectionUtil.toList(dbIterator);
    }

    private List<ExchangeContract> getAllByLongParameterFromStatus(Long parameterValue, String parameterName, int fromStatus) {
        DbIterator<ExchangeContract> dbIterator = getManyBy(new DbClause.LongClause(parameterName, parameterValue).and(new DbClause.ByteClause("status", DbClause.Op.GTE, (byte) fromStatus)), 0, -1);
        return CollectionUtil.toList(dbIterator);
    }

    public List<ExchangeContract> getByOrderIdAndHeight(long orderId, int height) throws AplException.ExecutiveProcessException {
        try (Connection con = getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con
                 .prepareStatement("SELECT * FROM dex_contract  where latest = true " +
                     "AND height = ? AND (offer_id = ? OR counter_offer_id = ?)")
        ) {
            int i = 0;
            pstmt.setInt(++i, height);
            pstmt.setLong(++i, orderId);
            pstmt.setLong(++i, orderId);

            return CollectionUtil.toList(getManyBy(con, pstmt, true));
        } catch (SQLException ex) {
            throw new AplException.ExecutiveProcessException(ex.getMessage(), ex);
        }
    }

    public ExchangeContract getLastByOrder(Long orderId) {
        List<ExchangeContract> allByOrder = getAllByLongParameterFromStatusHeightSorted(orderId, "offer_id", 1);
        return getFirstOrNull(allByOrder);
    }

    public ExchangeContract getLastByCounterOrder(Long orderId) {
        List<ExchangeContract> allByOrder = getAllByLongParameterFromStatusHeightSorted(orderId, "counter_offer_id", 1);
        return getFirstOrNull(allByOrder);
    }

    public ExchangeContract getByOrderAndCounterOrder(Long orderId, Long counterOrderId) {
        // impossible to match to the same order multiple times,
        // so that contract for pair of counter order and order is always unique
        return getBy(new DbClause.LongClause("counter_offer_id", counterOrderId).and(new DbClause.LongClause("offer_id", orderId)));
    }

    public List<ExchangeContract> getOverdueContractsStep1and2(int deadlineToReply) throws AplException.ExecutiveProcessException {
        String sql = "SELECT * FROM dex_contract  where latest = true " +
            "AND status IN (0,1) AND deadline_to_reply < ?";

        return getOverdueContracts(deadlineToReply, sql);
    }

    public List<ExchangeContract> getOverdueContractsStep1_2_3(int deadlineToReply) throws AplException.ExecutiveProcessException {
        String sql = "SELECT * FROM dex_contract  where latest = true " +
            "AND status IN (0,1,2) AND deadline_to_reply < ?";

        return getOverdueContracts(deadlineToReply, sql);
    }

    private List<ExchangeContract> getOverdueContracts(int deadlineToReply, String sql) throws AplException.ExecutiveProcessException {
        try (Connection con = getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con
                 .prepareStatement(sql)
        ) {
            int i = 0;
            pstmt.setLong(++i, deadlineToReply);

            DbIterator<ExchangeContract> contracts = getManyBy(con, pstmt, true);

            return CollectionUtil.toList(contracts);
        } catch (SQLException ex) {
            throw new AplException.ExecutiveProcessException(ex.getMessage(), ex);
        }
    }

}
