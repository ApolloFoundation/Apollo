package com.apollocurrency.aplwallet.apl.core.db.cdi.transaction;

import static org.apache.uima.UIMAFramework.getLogger;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.slf4j.Logger;

/**
 * CDI interceptor for {@link Transactional} annotation.
 * Opens connection and starts transaction for methods with {@link Transactional} annotation.
 */
@Transactional
@Interceptor
public class JdbiTransactionalInterceptor {
    private static final Logger log = getLogger(JdbiHandleFactory.class);

    @Inject
    private JdbiHandleFactory jdbiHandleFactory;

    @AroundInvoke
    public Object invokeInTransaction(InvocationContext ctx) throws Exception {

        if (jdbiHandleFactory.isInTransaction()) {
            return ctx.proceed();
        }

        Transactional annotation = ctx.getMethod().getAnnotation(Transactional.class);
        Transaction annotation2 = ctx.getMethod().getAnnotation(Transaction.class);

        jdbiHandleFactory.open();

        try {
            jdbiHandleFactory.begin();

            Object result = ctx.proceed();

            if ( (annotation != null && annotation.readOnly())
                || (annotation2 != null && annotation2.readOnly()) ) {
                jdbiHandleFactory.rollback();
            } else {
                jdbiHandleFactory.commit();
            }

            return result;
        } catch (Exception e) {
            jdbiHandleFactory.rollback();
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            jdbiHandleFactory.close();
        }
    }

}
