/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p;

/**
 * Enum for file state while downloding
 */
public enum FileChunkInfoPresent {

    /**
     * 0 - not present
     * 1 - download in progress;
     * 2 - present;
     * 3 - saved;
     */
    NOT_PRESENT(0),
    DOWNLOAD_IN_PROCGRESS(1),
    PRESENT(2),
    SAVED(3);

    private int value;

    FileChunkInfoPresent(int value) {
        this.value = value;
    }
}
