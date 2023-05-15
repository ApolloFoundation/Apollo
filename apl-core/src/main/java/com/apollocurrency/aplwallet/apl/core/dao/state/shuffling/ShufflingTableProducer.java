/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ShufflingTableProducer {
    @Inject
    DatabaseManager databaseManager;
    @Inject
    Event<FullTextOperationData> fullTextOperationDataEvent;


    public ShufflingTable shufflingTable() {
        return new ShufflingTable(databaseManager, fullTextOperationDataEvent);
    }
}
