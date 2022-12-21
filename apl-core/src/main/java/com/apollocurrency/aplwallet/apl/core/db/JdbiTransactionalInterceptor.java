/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import static org.slf4j.LoggerFactory.getLogger;

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

        boolean readOnly = annotation.readOnly();
        boolean createHandle = !jdbiHandleFactory.currentHandleOpened();
        if (createHandle) {
            jdbiHandleFactory.open();
            logIfTraceEnabled("Open handle {}.{}", ctx.getTarget(), ctx.getMethod().getName());
        }
        try {
            if (!readOnly) {
                if (!createHandle && jdbiHandleFactory.isReadOnly()) {
                    log.warn("Will start transaction on readOnly handle");
                }
                jdbiHandleFactory.begin();
                logIfTraceEnabled("Begin transaction {}.{}", ctx.getTarget(), ctx.getMethod().getName());
            } else {
                if (createHandle) {
                    jdbiHandleFactory.setReadOnly(true);
                }
            }
            Object result = ctx.proceed();

            if (!readOnly) {
                jdbiHandleFactory.commit();
                logIfTraceEnabled("Commit transaction {}.{}", ctx.getTarget(), ctx.getMethod().getName());
            }

            return result;
        } catch (Exception e) {
            if (!readOnly) {
                jdbiHandleFactory.rollback();
                logIfTraceEnabled("Rollback transaction {}.{}", ctx.getTarget(), ctx.getMethod().getName());
            }
            throw e;
        } finally {
            if (createHandle) {
                jdbiHandleFactory.close();
                logIfTraceEnabled("Close handle {}.{}", ctx.getTarget(), ctx.getMethod().getName());
            }
        }
    }

    private void logIfTraceEnabled(String pattern, Object... objects) {
        if (log.isTraceEnabled()) {
            log.trace(pattern, objects);
        }
    }
}
