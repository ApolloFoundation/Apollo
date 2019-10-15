package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DexOrderMapper;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implemented for backward compatibility with rollback function in the DerivedDbTable.
 * Use DexOfferDao for not transactional operations. (f.e. search)
 */
@Singleton
@Slf4j
public class DexOrderTable extends EntityDbTable<DexOrder> {



    private static final String TABLE_NAME = "dex_offer";
    private final Blockchain blockchain;
    private DexOrderMapper dexOrderMapper;
    private DexOrderKeyFactory keyFactory;

    @Inject
    public DexOrderTable(Blockchain blockchain, DexOrderMapper dexOrderMapper, DexOrderKeyFactory keyFactory) {
        super(TABLE_NAME, keyFactory, true, null,false);
        this.keyFactory = keyFactory;
        this.dexOrderMapper = dexOrderMapper;
        this.blockchain = Objects.requireNonNull(blockchain, "Blockchain is NULL");
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

    @Override
    public void save(Connection con, DexOrder order) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO dex_offer (id, account_id, type, " +
                "offer_currency, offer_amount, pair_currency, pair_rate, finish_time, status, height, latest, from_address, to_address)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?)")){
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
            pstmt.setInt(++i, blockchain.getHeight());
            pstmt.setString(++i, order.getFromAddress());
            pstmt.setString(++i, order.getToAddress());
            pstmt.executeUpdate();
        }
    }
}
