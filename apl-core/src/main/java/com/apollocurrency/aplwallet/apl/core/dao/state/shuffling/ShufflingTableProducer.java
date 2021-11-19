/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

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
