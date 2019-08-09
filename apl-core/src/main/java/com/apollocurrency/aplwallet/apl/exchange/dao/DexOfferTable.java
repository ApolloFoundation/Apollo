package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DexOfferMapper;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Implemented for backward compatibility with rollback function in the DerivedDbTable.
 * Use DexOfferDao for not transactional operations. (f.e. search)
 */
@Deprecated
@Singleton
public class DexOfferTable  extends EntityDbTable<DexOffer> {

    private static final Logger LOG = LoggerFactory.getLogger(DexOfferTable.class);

    static final LongKeyFactory<DexOffer> KEY_FACTORY = new LongKeyFactory<>("transaction_id") {
        @Override
        public DbKey newKey(DexOffer offer) {
            return new LongKey(offer.getTransactionId());
        }
    };


    private static final String TABLE_NAME = "dex_offer";
    private final Blockchain blockchain;
    private DexOfferMapper dexOfferMapper;

    @Inject
    public DexOfferTable(Blockchain blockchain, DexOfferMapper dexOfferMapper) {
        super(TABLE_NAME, KEY_FACTORY, true, null,false);
        this.dexOfferMapper = dexOfferMapper;
        this.blockchain = Objects.requireNonNull(blockchain, "Blockchain is NULL");
    }

    @Override
    public DexOffer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return dexOfferMapper.map(rs, null);
    }

    @Override
    public void save(Connection con, DexOffer offer) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO dex_offer (transaction_id, account_id, type, " +
                "offer_currency, offer_amount, pair_currency, pair_rate, finish_time, status, height, latest, from_address, to_address)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?)")){
            int i = 0;
            pstmt.setLong(++i, offer.getTransactionId());
            pstmt.setLong(++i, offer.getAccountId());
            pstmt.setByte(++i, (byte) offer.getType().ordinal());
            pstmt.setByte(++i, (byte) offer.getOfferCurrency().ordinal());
            pstmt.setLong(++i, offer.getOfferAmount());
            pstmt.setByte(++i, (byte) offer.getPairCurrency().ordinal());
            //TODO change type in the db
            pstmt.setLong(++i, EthUtil.ethToGwei(offer.getPairRate()));
            pstmt.setInt(++i, offer.getFinishTime());
            pstmt.setByte(++i, (byte) offer.getStatus().ordinal());
            pstmt.setInt(++i, blockchain.getHeight());
            pstmt.setString(++i, offer.getFromAddress());
            pstmt.setString(++i, offer.getToAddress());
            pstmt.executeUpdate();
        }
    }
}
