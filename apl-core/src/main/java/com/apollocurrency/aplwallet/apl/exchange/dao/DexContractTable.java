package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.ExchangeContractMapper;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.util.AplException;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Use DexContractDao for not transactional operations. ( f.e. search)
 */
@Slf4j
public class DexContractTable  extends VersionedDeletableEntityDbTable<ExchangeContract> {

    private static ExchangeContractMapper exchangeContractMapper = new ExchangeContractMapper();

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
                "sender, recipient, secret_hash, encrypted_secret, transfer_tx_id, counter_transfer_tx_id, deadline_to_reply, status, height, latest) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
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
            pstmt.setInt(++i, blockchain.getHeight());

            pstmt.executeUpdate();
        }
    }

    public ExchangeContract getById(Long id) {
        return get(KEY_FACTORY.newKey(id));
    }

    public List<ExchangeContract> getAllByCounterOrder(Long orderId) {
        DbIterator<ExchangeContract> dbIterator = getManyBy(new DbClause.LongClause("counter_offer_id", orderId), 0, -1);
        return CollectionUtil.toList(dbIterator);
    }

    public ExchangeContract getByOrder(Long orderId) {
        return getBy(new DbClause.LongClause("offer_id", orderId));
    }

    public ExchangeContract getByCounterOrder(Long orderId) {
        return getBy(new DbClause.LongClause("counter_offer_id", orderId));
    }

    public ExchangeContract getByOrderAndCounterOrder(Long orderId, Long counterOrderId) {
        return getBy(new DbClause.LongClause("counter_offer_id", counterOrderId).and(new DbClause.LongClause("offer_id", orderId)));
    }

    public List<ExchangeContract> getOverdueContractsStep1and2(int deadlineToReply) throws AplException.ExecutiveProcessException {
        try (Connection con = getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con
                     .prepareStatement("SELECT * FROM dex_contract  where latest = true " +
                             "AND status IN (0,1) AND deadline_to_reply < ?")
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
