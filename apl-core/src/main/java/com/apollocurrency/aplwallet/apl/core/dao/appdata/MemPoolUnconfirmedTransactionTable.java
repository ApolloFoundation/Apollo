/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DbTableWrapper;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransactionEntity;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class MemPoolUnconfirmedTransactionTable extends DbTableWrapper<UnconfirmedTransactionEntity> {
    private final UnconfirmedTransactionTable unconfirmedTransactionTable;

    @Inject
    public MemPoolUnconfirmedTransactionTable(UnconfirmedTransactionTable table) {
        super(table);
        this.unconfirmedTransactionTable = table;
    }

    public UnconfirmedTransactionEntity getById(long id) {
        return unconfirmedTransactionTable.get(unconfirmedTransactionTable.getTransactionKeyFactory().newKey(id));
    }

    public boolean deleteById(long id) {
        return unconfirmedTransactionTable.deleteById(id) > 0;
    }

    public List<Long> getAllUnconfirmedTransactionIds() {
        return unconfirmedTransactionTable.getAllUnconfirmedTransactionIds();
    }

    public int countExpiredTransactions(int epochTime) {
        return unconfirmedTransactionTable.countExpiredTransactions(epochTime);
    }

    public Stream<UnconfirmedTransactionEntity> getAllUnconfirmedTransactionsStream() {
        return unconfirmedTransactionTable.getAllUnconfirmedTransactions();
    }

    public Stream<UnconfirmedTransactionEntity> getExpiredTxsStream(int epochTime) {
        return unconfirmedTransactionTable.getExpiredTxsStream(epochTime);
    }
}
