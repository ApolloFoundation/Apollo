/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DbTableWrapper;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class MemPoolUnconfirmedTransactionTable extends DbTableWrapper<UnconfirmedTransaction> {
    private final UnconfirmedTransactionTable table;
    @Inject
    public MemPoolUnconfirmedTransactionTable(UnconfirmedTransactionTable table) {
        super(table);
        this.table = table;
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
        return 0;
    }

    public Stream<UnconfirmedTransaction> getExpiredTxsStream(int epochTime) {
        return table.getExpiredTxsStream(epochTime);
    }
}
