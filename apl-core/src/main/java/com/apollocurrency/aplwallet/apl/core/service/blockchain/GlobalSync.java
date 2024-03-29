/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

public interface GlobalSync {
    void readLock();

    void readUnlock();

    void updateLock();

    void updateUnlock();

    void writeLock();

    void writeUnlock();
}
