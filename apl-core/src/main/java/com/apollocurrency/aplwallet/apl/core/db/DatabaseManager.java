package com.apollocurrency.aplwallet.apl.core.db;

import javax.enterprise.inject.Produces;

import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jdbi.v3.core.Jdbi;

public interface DatabaseManager {

    TransactionalDataSource getDataSource();

    @Produces
    Jdbi getJdbi();

    DbProperties getBaseDbProperties();

    PropertiesHolder getPropertiesHolder();

    void shutdown();

    void shutdown(TransactionalDataSource dataSource);
}
