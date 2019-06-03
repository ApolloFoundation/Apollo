/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

public class IndexExportData {
    private long txExported;
    private long blocksExported;

    public IndexExportData(long txExported, long blocksExported) {
        this.txExported = txExported;
        this.blocksExported = blocksExported;
    }

    public long getTxExported() {
        return txExported;
    }

    public void setTxExported(long txExported) {
        this.txExported = txExported;
    }

    public long getBlocksExported() {
        return blocksExported;
    }

    public void setBlocksExported(long blocksExported) {
        this.blocksExported = blocksExported;
    }
}
