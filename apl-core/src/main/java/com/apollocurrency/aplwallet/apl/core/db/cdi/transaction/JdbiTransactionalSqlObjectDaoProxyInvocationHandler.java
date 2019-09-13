package com.apollocurrency.aplwallet.apl.core.db.cdi.transaction;

import org.jdbi.v3.core.Handle;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Separates DAO from JDBI, gives possibility to inject DAO object into service.
 */
public class JdbiTransactionalSqlObjectDaoProxyInvocationHandler<DAO> implements InvocationHandler {

    private final JdbiHandleFactory jdbiHandleFactory;
    private final Class<DAO> daoClass;

    private JdbiTransactionalSqlObjectDaoProxyInvocationHandler(JdbiHandleFactory jdbiHandleFactory, Class<DAO> daoClass) {
        this.jdbiHandleFactory = jdbiHandleFactory;
        this.daoClass = daoClass;
    }

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(JdbiHandleFactory jdbiHandleFactory, Class<T> daoClass) {
        return (T) Proxy.newProxyInstance(daoClass.getClassLoader(), new Class[]{daoClass},
                new JdbiTransactionalSqlObjectDaoProxyInvocationHandler<T>(jdbiHandleFactory, daoClass));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
        Handle handle = jdbiHandleFactory.getCurrentHandle();
        boolean closeHandle = handle == null;
        if (closeHandle) {
            handle = jdbiHandleFactory.getOrOpenHandle();
        }
        DAO dao = handle.attach(daoClass);
        try {
            return method.invoke(dao, args);
        } finally {
            if (closeHandle) {
                jdbiHandleFactory.close();
            }
        }
    }
}
