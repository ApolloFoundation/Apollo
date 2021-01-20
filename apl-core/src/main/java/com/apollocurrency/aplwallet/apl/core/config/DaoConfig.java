/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexCandlestickDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOperationDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.MandatoryTransactionDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.OrderScanDao;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Configuration DAO classes with Jdbi, Transaction resources
 */
@Singleton
@SuppressWarnings("unused")
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
    private DexOrderDao dexOfferDao() {
        return createDaoInterfaceProxy(DexOrderDao.class);
    }


    @Produces
    private DexContractDao dexContractDao() {
        return createDaoInterfaceProxy(DexContractDao.class);
    }

    @Produces
    private MandatoryTransactionDao mandatoryTransactionDao() {
        return createDaoInterfaceProxy(MandatoryTransactionDao.class);
    }

    @Produces
    private DexCandlestickDao candlestickDao() {
        return createDaoInterfaceProxy(DexCandlestickDao.class);
    }

    @Produces
    private OrderScanDao orderScanDao() {
        return createDaoInterfaceProxy(OrderScanDao.class);
    }

    @Produces
    private DexOperationDao dexOperationDao() {
        return createDaoInterfaceProxy(DexOperationDao.class);
    }

    private <T> T createDaoInterfaceProxy(Class<T> daoClass) {
        return JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(
            jdbiHandleFactory,
            daoClass
        );
    }

}
