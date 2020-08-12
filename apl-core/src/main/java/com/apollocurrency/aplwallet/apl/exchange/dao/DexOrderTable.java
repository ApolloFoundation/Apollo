/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.converter.db.DexOrderMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implemented for backward compatibility with rollback function in the DerivedDbTable.
 * Use DexOfferDao for not transactional operations. (f.e. search)
 * DEX trade in derived table hierarchy is used for exporting/importing shard data.
 */
@Singleton
@Slf4j
public class DexOrderTable extends EntityDbTable<DexOrder> {

    private static final String TABLE_NAME = "dex_offer";
    private static DexOrderKeyFactory keyFactory = new DexOrderKeyFactory();
    private DexOrderMapper dexOrderMapper = new DexOrderMapper();

    @Inject
    public DexOrderTable() {
        super(TABLE_NAME, keyFactory, true, null, false);
    }

    @Override
    public DexOrder load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return dexOrderMapper.map(rs, null);
    }

    public DexOrder getByTxId(Long transactionId) {
        return get(keyFactory.newKey(transactionId));
    }

    public List<DexOrder> getOverdueOrders(int currentTime) {
        List<DexOrder> dexOrders = new ArrayList<>();
        try (Connection con = getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con
                 .prepareStatement("SELECT * FROM dex_offer AS offer where latest = true " +
                     "AND offer.status = 0 AND offer.finish_time < ?")
        ) {
            int i = 0;
            pstmt.setLong(++i, currentTime);

            DbIterator<DexOrder> orders = getManyBy(con, pstmt, true);

            return CollectionUtil.toList(orders);
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        }

        return dexOrders;
    }

    public List<DexOrder> getWaitingPhasingResultOrders() {
        return CollectionUtil.toList(getManyBy(new DbClause.ByteClause("status", (byte) OrderStatus.PHASING_RESULT_PENDING.ordinal()), 0, -1));
    }

    @Override
    public void save(Connection con, DexOrder order) throws SQLException {
        log.debug("Save new order: id:{}, accountId:{}, type:{}, order_currency:{}, :{}, pair_currency:{}, " +
                "finish_time:{}, status:{}, height:{}, from_address:{}, to_address:{}",
            order.getId(), order.getAccountId(), order.getType(), order.getOrderCurrency(), order.getOrderAmount(), order.getPairCurrency(),
            order.getFinishTime(), order.getStatus(), order.getHeight(), order.getFromAddress(), order.getToAddress());

        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO dex_offer (id, account_id, `type`, " +
            "offer_currency, offer_amount, pair_currency, pair_rate, finish_time, status, height, latest, from_address, to_address) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?) "
            + "ON DUPLICATE KEY UPDATE id = VALUES(id), account_id = VALUES(account_id), `type` = VALUES(`type`), "
            + "offer_currency = VALUES(offer_currency), offer_amount = VALUES(offer_amount), pair_currency = VALUES(pair_currency), "
            + "pair_rate = VALUES(pair_rate), finish_time = VALUES(finish_time), status = VALUES(status), "
            + "height = VALUES(height), latest = TRUE , from_address = VALUES(from_address), to_address = VALUES(to_address)")
        ) {

            int i = 0;
            pstmt.setLong(++i, order.getId());
            pstmt.setLong(++i, order.getAccountId());
            pstmt.setByte(++i, (byte) order.getType().ordinal());
            pstmt.setByte(++i, (byte) order.getOrderCurrency().ordinal());
            pstmt.setLong(++i, order.getOrderAmount());
            pstmt.setByte(++i, (byte) order.getPairCurrency().ordinal());
            //TODO change type in the db
            pstmt.setLong(++i, EthUtil.ethToGwei(order.getPairRate()));
            pstmt.setInt(++i, order.getFinishTime());
            pstmt.setByte(++i, (byte) order.getStatus().ordinal());
            pstmt.setInt(++i, order.getHeight());
            pstmt.setString(++i, order.getFromAddress());
            pstmt.setString(++i, order.getToAddress());
            pstmt.executeUpdate();
        }
    }

    public List<DexOrder> getPendingOrdersWithoutContracts(int height) {
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstm = con.prepareStatement(
                 "SELECT * FROM dex_offer LEFT JOIN dex_contract ON dex_offer.id = dex_contract.counter_offer_id " +
                     "OR dex_offer.id = dex_contract.offer_id WHERE dex_contract.id IS NULL AND dex_offer.status=1 AND dex_offer.height < ? AND dex_offer.latest = true")) {
            pstm.setInt(1, height);
            return CollectionUtil.toList(getManyBy(con, pstm, false));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
