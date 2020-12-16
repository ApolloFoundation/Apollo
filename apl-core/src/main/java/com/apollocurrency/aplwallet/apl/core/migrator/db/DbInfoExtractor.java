/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

/**
 * Extract db info such as blockchain height and actual path
 */
public interface DbInfoExtractor {

    /**
     * Extract height of the blockchain from the db specified by dbPath
     *
     * @param dbPath path to the target db
     * @return height of the blockchain in the target db or 0 when no blocks in db or when any db error occurred
     */
    int getHeight();

}
