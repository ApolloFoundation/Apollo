package com.apollocurrency.aplwallet.apl.core.db.cdi.transaction;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;

import javax.inject.Singleton;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Holds opened connection {@link Handle} for current thread.
 */
@Singleton
public class JdbiHandleFactory {
    private static final Logger log = getLogger(JdbiHandleFactory.class);

    private final static ThreadLocal<Handle> currentHandleThreadLocal = new ThreadLocal<>();

    private volatile Jdbi jdbi;

    public void setJdbi(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public boolean isInTransaction() {
        Handle handle = currentHandleThreadLocal.get();
        return handle != null && handle.isInTransaction();
    }

    public Handle getOrOpenHandle() {
        Handle handle = getCurrentHandle();
        if (handle == null) {
            handle = open();
        }
        return handle;
    }

    public Handle getCurrentHandle() {
        return currentHandleThreadLocal.get();
    }

    public Handle open() {
        Handle handle = getCurrentHandle();
        if (handle != null) {
            throw new IllegalStateException("Unable to open new handle. Previous is still opened");
        }
        handle = jdbi.open();
        currentHandleThreadLocal.set(handle);
        return handle;
    }

    protected void begin() {
        Handle handle = requireOpenHandle("begin");
        handle.begin();
    }

    private Handle requireOpenHandle(String action) {
        Handle handle = getCurrentHandle();
        if (handle == null) {
            throw new IllegalStateException("Unable to " + action + ". Handle is null");
        } else {
            return handle;
        }
    }

    protected boolean isReadOnly() {
        return currentHandleOpened() && getCurrentHandle().isReadOnly();
    }

    protected void setReadOnly(boolean readOnly) {
        Handle handle = requireOpenHandle("SetReadOnly");
        if (handle.isInTransaction()) {
            throw new IllegalStateException("Unable to set read only for handle in transaction");
        }
        if (readOnly != handle.isReadOnly()) {
            handle.setReadOnly(readOnly);
        }
    }

    protected boolean currentHandleOpened() {
        return getCurrentHandle() != null;
    }

    protected void commit() {
        Handle handle = requireOpenHandle("commit");
        handle.commit();
    }

    protected void rollback() {
        Handle handle = requireOpenHandle("rollback");
        handle.rollback();
    }

    public void close() {
        Handle handle = requireOpenHandle("close");
        handle.close();
        currentHandleThreadLocal.remove();
    }

}
