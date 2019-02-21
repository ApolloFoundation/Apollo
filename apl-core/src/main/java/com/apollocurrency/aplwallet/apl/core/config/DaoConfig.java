package com.apollocurrency.aplwallet.apl.core.config;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.core.db.dao.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;

/**
 * Configuration DAO classes with Jdbi, Transaction resources
 */
@Singleton
public class DaoConfig {

    private JdbiHandleFactory jdbiHandleFactory;

    @Inject
    public void setJdbiHandleFactory(JdbiHandleFactory jdbiHandleFactory) {
        this.jdbiHandleFactory = jdbiHandleFactory;
    }

    @Produces
    private ShardDao shardDao() {
        return createDaoInterfaceProxy(ShardDao.class);
    }

    @Produces
    private BlockIndexDao blockIndexDao() {
        return createDaoInterfaceProxy(BlockIndexDao.class);
    }

    private <T> T createDaoInterfaceProxy(Class<T> daoClass) {
        return JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(
                jdbiHandleFactory,
                daoClass
        );
    }

}
