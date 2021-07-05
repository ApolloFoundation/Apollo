/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.util.db.DataSourceCreator;
import com.apollocurrency.aplwallet.apl.util.db.DatabaseAdministratorFactory;
import com.apollocurrency.aplwallet.apl.util.db.DatabaseAdministratorFactoryImpl;
import com.apollocurrency.aplwallet.apl.util.db.SelfInitializableDataSourceCreator;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DbBeanConfiguration {
    private final DirProvider dirProvider;

    @Inject
    public DbBeanConfiguration(DirProvider dirProvider) {
        this.dirProvider = dirProvider;
    }


    @Produces
    @Singleton
    public DatabaseAdministratorFactory dbAdminFactory() {
        return new DatabaseAdministratorFactoryImpl(dirProvider);
    }

    @Produces
    @Singleton
    public DataSourceCreator dataSourceCreator() {
        return new SelfInitializableDataSourceCreator(dbAdminFactory());
    }
}
