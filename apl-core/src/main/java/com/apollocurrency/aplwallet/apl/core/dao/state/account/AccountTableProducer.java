/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class AccountTableProducer {
    private final DatabaseManager databaseManager;
    private final Event<FullTextOperationData> event;

    @Inject
    public AccountTableProducer(DatabaseManager databaseManager,
                                Event<FullTextOperationData> event) {
        this.databaseManager = databaseManager;
        this.event = event;
    }

    public AccountTable accountTable() {
        return new AccountTable(databaseManager, event);
    }
}
