package com.apollocurrency.aplwallet.apl.dex.config;

import com.apollocurrency.aplwallet.apl.dex.core.dao.DexTransactionDao;
import com.apollocurrency.aplwallet.apl.dex.core.dao.UserErrorMessageDao;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DexDaoConfig {

    private JdbiHandleFactory jdbiHandleFactory;

    @Inject
    public void setJdbiHandleFactory(JdbiHandleFactory jdbiHandleFactory) {
        this.jdbiHandleFactory = jdbiHandleFactory;
    }

    @Produces
    private DexTransactionDao dexTransactionDao() {
        return createDaoInterfaceProxy(DexTransactionDao.class);
    }

    @Produces
    private UserErrorMessageDao userErrorMessage() {
        return createDaoInterfaceProxy(UserErrorMessageDao.class);
    }

    private <T> T createDaoInterfaceProxy(Class<T> daoClass) {
        return JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(
            jdbiHandleFactory,
            daoClass
        );
    }
}
