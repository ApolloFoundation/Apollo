package com.apollocurrency.aplwallet.apl.util.cdi.transaction;

import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.CloseException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.transaction.TransactionException;

/**
 * Holds opened connection {@link Handle} for current thread.
 */
@Slf4j
public class JdbiHandleFactory {

    private final static ThreadLocal<Handle> currentHandleThreadLocal = new ThreadLocal<>();

    private volatile Jdbi jdbi;

    public void setJdbi(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public JdbiHandleFactory(Jdbi jdbi) {
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

    public void begin() {
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

    public boolean isReadOnly() {
        return currentHandleOpened() && getCurrentHandle().isReadOnly();
    }

    public void setReadOnly(boolean readOnly) {
        Handle handle = requireOpenHandle("SetReadOnly");
        if (handle.isInTransaction()) {
            throw new IllegalStateException("Unable to set read only for handle in transaction");
        }
        if (readOnly != handle.isReadOnly()) {
            handle.setReadOnly(readOnly);
        }
    }

    public boolean currentHandleOpened() {
        return getCurrentHandle() != null;
    }

    public void commit() {
        Handle handle = requireOpenHandle("commit");
        handle.commit();
    }

    public void rollback() {
        Handle handle = requireOpenHandle("rollback");
        handle.rollback();
    }

    public void close() {
        Handle handle = requireOpenHandle("close");
        try {
            handle.close();
        } catch (CloseException e) {
            log.error("Error during closing active handle", e);
        } catch (TransactionException e) {
            log.error("Fatal error, transaction is active and not committed/rolled back. Will rollback it entirely: {}", e.getMessage());
            throw new IllegalStateException("Transaction is not finished before close", e);
        } finally {
            currentHandleThreadLocal.remove();
        }
    }

}
