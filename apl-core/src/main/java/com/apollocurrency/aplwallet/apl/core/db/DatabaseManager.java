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

    void shutdown();

    void setAvailable(boolean available);

    UUID getChainId();
}
