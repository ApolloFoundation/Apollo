package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.ExchangeContractMapper;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Deprecated
public class DexContractTable   extends EntityDbTable<ExchangeContract> {
    private static final Logger LOG = LoggerFactory.getLogger(DexContractTable.class);

    static final LongKeyFactory<ExchangeContract> KEY_FACTORY = new LongKeyFactory<>("db_id") {
        @Override
        public DbKey newKey(ExchangeContract offer) {
            return new LongKey(offer.getOrderId());
        }
    };

    private static final String TABLE_NAME = "dex_contract";
    private ExchangeContractMapper mapper;
    private Blockchain blockchain;

    @Inject
    public DexContractTable(ExchangeContractMapper mapper, Blockchain blockchain) {
        super(TABLE_NAME, KEY_FACTORY);
        this.mapper = mapper;
        this.blockchain = blockchain;
    }

    @Override
    protected ExchangeContract load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return mapper.map(rs, null);
    }

    @Override
    public void save(Connection con, ExchangeContract entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO dex_contract (offer_id, counter_offer_id, secret_hash, " +
                " height, latest) " +
                "VALUES (?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, entity.getOrderId());
            pstmt.setLong(++i, entity.getCounterOrderId());
            pstmt.setString(++i, entity.getSecretHash());
            pstmt.setInt(++i, blockchain.getHeight());

            pstmt.executeUpdate();
        }
    }
}
