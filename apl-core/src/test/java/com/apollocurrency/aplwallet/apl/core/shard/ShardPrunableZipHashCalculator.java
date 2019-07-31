/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExporter;
import com.apollocurrency.aplwallet.apl.util.Zip;

public class ShardPrunableZipHashCalculator {
    private DerivedTablesRegistry registry;
    private CsvExporter csvExporter;
    private Zip zip;

//    public byte[] calculatePrunableArchiveHash(int height, int time) {
//        List<DerivedTableInterface> prunableTables = registry.getDerivedTables().stream().filter(t -> t instanceof PrunableDbTable).forEach(t-> csvExporter.exportDerivedTable());
//
//    }
}
