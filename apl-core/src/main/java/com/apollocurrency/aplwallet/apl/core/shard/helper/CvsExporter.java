/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;

/**
 * Helper interface for exporting table data from one Db into CSV
 *
 * @author yuriy.larin
 */
public interface CvsExporter {

    /**
     * Exports one specified table and returns number of exported rows
     * The CSV file put into folder specified by implementation component
     *
     * @param tableInterface table tp export
     * @param targetHeight target blockchain height
     * @param batchLimit rows in batch to process
     * @return exported quantity
     */
    long exportDerivedTable(DerivedTableInterface tableInterface, int targetHeight, int batchLimit);

    /**
     * Exports one SHARD table and returns number of exported rows
     * The CSV file put into folder specified by implementation component.
     *
     * @param targetHeight target blockchain height
     * @param batchLimit rows in batch to process
     * @return exported quantity
     */
    long exportShardTable(int targetHeight, int batchLimit);

}
