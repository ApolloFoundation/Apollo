package com.apollocurrency.aplwallet.apl.core.db.cdi.transaction;

import static org.apache.uima.UIMAFramework.getLogger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;

/**
 * Holds opened connection {@link Handle} for current thread.
 */
@Singleton
public class JdbiHandleFactory {
    private static final Logger log = getLogger(JdbiHandleFactory.class);

    private final static ThreadLocal<Handle> currentHandleThreadLocal = new ThreadLocal<>();

    private Jdbi jdbi;

    @Inject
    public void setJdbi(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public boolean isInTransaction() {
        Handle handle = currentHandleThreadLocal.get();
        return handle != null && handle.isInTransaction();
    }

    public Handle getCurrentHandle() {
        Handle handle = currentHandleThreadLocal.get();
        if (handle == null) {
            handle = open();
        }
        return handle;
    }

    protected Handle open() {
        Handle handle = currentHandleThreadLocal.get();
        if (handle != null) {
            return handle;
        }
        handle = jdbi.open();
        currentHandleThreadLocal.set(handle);
        return handle;
    }

    protected void begin() {
        Handle handle = getCurrentHandle();
        handle.begin();
    }

    protected void commit() {
        Handle handle = getCurrentHandle();
        handle.commit();
    }

    protected void rollback() {
        Handle handle = getCurrentHandle();
        handle.rollback();
    }

    public void close() {
        Handle handle = getCurrentHandle();
        handle.close();
        currentHandleThreadLocal.remove();
    }

}
