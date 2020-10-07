/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DbTableWrapper;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPoolInMemoryState;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class MemPoolUnconfirmedTransactionTable extends DbTableWrapper<UnconfirmedTransaction> {
    private final UnconfirmedTransactionTable table;
    private final MemPoolInMemoryState memPoolInMemoryState;
    @Inject
    public MemPoolUnconfirmedTransactionTable(UnconfirmedTransactionTable table, MemPoolInMemoryState memPoolInMemoryState) {
        super(table);
        this.table = table;
        this.memPoolInMemoryState = memPoolInMemoryState;
    }

    public UnconfirmedTransaction getById(long id) {
        return table.get(table.getTransactionKeyFactory().newKey(id));
    }

    public boolean deleteById(long id) {
        return table.deleteById(id) > 0;
    }
    public List<Long> getAllUnconfirmedTransactionIds() {
        return table.getAllUnconfirmedTransactionIds();
    }

    public int countExpiredTransactions(int epochTime) {
        return table.countExpiredTransactions(epochTime);
    }

    @Override
    public int rollback(int height) {
        // rollback nothing
//        int rc;
//        try (Connection con = table.getDatabaseManager().getDataSource().getConnection();
//             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM unconfirmed_transaction WHERE height > ?")) {
//            pstmt.setInt(1, height);
//            try (ResultSet rs = pstmt.executeQuery()) {
//                while (rs.next()) {
//                    UnconfirmedTransaction unconfirmedTransaction = table.load(con, rs, null);
//                    memPoolInMemoryState.backToWaiting(unconfirmedTransaction);
//                    log.trace("Revert to waiting tx {}", unconfirmedTransaction.getId());
//                }
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e.toString(), e);
//        }
//        rc = super.rollback(height);
//        memPoolInMemoryState.resetUnconfirmedDuplicates();
//        return rc;
        return 0;
    }

    @Override
    public void insert(UnconfirmedTransaction entity) {
        super.insert(entity);
        memPoolInMemoryState.putInCache(entity);
    }

    public Stream<UnconfirmedTransaction> getExpiredTxsStream(int epochTime) {
        return table.getExpiredTxsStream(epochTime);
    }
}
