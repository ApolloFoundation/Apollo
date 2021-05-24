/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShufflingTableProducer {
    @Inject
    DatabaseManager databaseManager;
    @Inject
    Event<DeleteOnTrimData> deleteOnTrimDataEvent;


    @Produces
    @Singleton
    public ShufflingTable shufflingTable() {
        return new ShufflingTable(databaseManager, deleteOnTrimDataEvent);
    }
}
