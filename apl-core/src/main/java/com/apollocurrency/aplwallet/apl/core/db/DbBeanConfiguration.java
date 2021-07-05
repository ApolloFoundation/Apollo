/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.util.db.DataSourceCreator;
import com.apollocurrency.aplwallet.apl.util.db.DatabaseAdministratorFactory;
import com.apollocurrency.aplwallet.apl.util.db.DatabaseAdministratorFactoryImpl;
import com.apollocurrency.aplwallet.apl.util.db.SelfInitializableDataSourceCreator;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DbBeanConfiguration {
    private final DirProvider dirProvider;
//    private final PropertiesHolder propertiesHolder;

    @Inject
    public DbBeanConfiguration(DirProvider dirProvider/*, PropertiesHolder propertiesHolder*/) {
        this.dirProvider = dirProvider;
//        this.propertiesHolder = propertiesHolder;
    }


    @Produces
    @Singleton
    public DatabaseAdministratorFactory dbAdminFactory() {
        return new DatabaseAdministratorFactoryImpl(dirProvider);
    }

    @Produces
    @Singleton
    public DataSourceCreator dataSourceCreator() {
        return new SelfInitializableDataSourceCreator(dbAdminFactory()/*, propertiesHolder*/);
    }
}
