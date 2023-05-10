/*
 *  Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.service.fulltext;

/**
 * Interface to update Full Test Search index data by placing new data
 */
public interface FullTextSearchUpdater {

    void putFullTextOperationData(FullTextOperationData operationData);

}
