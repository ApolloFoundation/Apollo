package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jdbi.v3.core.Jdbi;

import java.util.UUID;

public interface DatabaseManager {

    TransactionalDataSource getDataSource();

    Jdbi getJdbi();

    JdbiHandleFactory getJdbiHandleFactory();

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
