/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AccountTableProducer {
    private final DatabaseManager databaseManager;
    private final Event<DeleteOnTrimData> event;

    @Inject
    public AccountTableProducer(DatabaseManager databaseManager, Event<DeleteOnTrimData> event) {
        this.databaseManager = databaseManager;
        this.event = event;
    }

    public AccountTable accountTable() {
        return new AccountTable(databaseManager, event);
    }
}
