/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import java.util.function.Supplier;

public class MultiLock {
    private final Object[] locks;

    public MultiLock(int size) {
        locks = new Object[size];
        for (int i = 0; i < size; i++) {
            locks[i] = new Object();
        }
    }

    public void inLockFor(Object objForLock, Runnable operation) {
        synchronized (locks[Math.abs(objForLock.hashCode() % locks.length)]) {
            operation.run();
        }
    }

    public <V> V inLockFor(Object objForLock, Supplier<V> operation) {
        synchronized (locks[Math.abs(objForLock.hashCode() % locks.length)]) {
            return operation.get();
        }
    }
}
