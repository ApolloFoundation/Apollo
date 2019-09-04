package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.ExchangeContractMapper;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.inject.Inject;

/**
 * Use DexContractDao for not transactional operations. ( f.e. search)
 */
public class DexContractTable  extends VersionedDeletableEntityDbTable<ExchangeContract> {

    static final LongKeyFactory<ExchangeContract> KEY_FACTORY = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(ExchangeContract exchangeContract) {
            return new LongKey(exchangeContract.getId());
        }
    };

    private static final String TABLE_NAME = "dex_contract";
    private ExchangeContractMapper mapper;
    private Blockchain blockchain;

    @Inject
    public DexContractTable(ExchangeContractMapper mapper, Blockchain blockchain) {
        super(TABLE_NAME, KEY_FACTORY, false);
        this.mapper = mapper;
        this.blockchain = blockchain;
    }

    @Override
    protected ExchangeContract load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return mapper.map(rs, null);
    }

    @Override
    public void save(Connection con, ExchangeContract entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO dex_contract (id, offer_id, counter_offer_id, " +
                "sender, recipient, secret_hash, encrypted_secret, transfer_tx_id, counter_transfer_tx_id, status, height, latest) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
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
            pstmt.setByte(++i, (byte) entity.getContractStatus().ordinal());
            pstmt.setInt(++i, blockchain.getHeight());

            pstmt.executeUpdate();
        }
    }
}
