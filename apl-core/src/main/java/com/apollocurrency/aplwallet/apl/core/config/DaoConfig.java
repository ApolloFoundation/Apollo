/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.core.db.dao.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexTradeDao;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

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

    @Produces
    private TransactionIndexDao transactionIndexDao() {
        return createDaoInterfaceProxy(TransactionIndexDao.class);
    }

    @Produces
    private ShardRecoveryDao shardRecoveryDao() {
        return createDaoInterfaceProxy(ShardRecoveryDao.class);
    }

    @Produces
    private DexOfferDao dexOfferDao() {
        return createDaoInterfaceProxy(DexOfferDao.class);
    }
    
    @Produces
    private DexTradeDao dexTradeDao() {
        return createDaoInterfaceProxy(DexTradeDao.class);
    }
    

    @Produces
    private DexContractDao dexContractDao() {
        return createDaoInterfaceProxy(DexContractDao.class);
    }

    private <T> T createDaoInterfaceProxy(Class<T> daoClass) {
        return JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(
                jdbiHandleFactory,
                daoClass
        );
    }

}
