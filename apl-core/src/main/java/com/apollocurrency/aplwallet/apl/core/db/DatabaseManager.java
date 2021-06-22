/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import java.util.UUID;

public interface DatabaseManager {

    TransactionalDataSource getDataSource();

    DbProperties getBaseDbProperties();

    PropertiesHolder getPropertiesHolder();

    /**
     * Shutdown main db and secondary shards.
     * After that the db can be reinitialized/opened again
     */
    void shutdown();

    void setAvailable(boolean available);

    UUID getChainId();
}
