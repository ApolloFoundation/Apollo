package com.apollocurrency.aplwallet.apl.core.service.fulltext;

/**
 * Interface to update Full Test Search index data by placing new data
 */
public interface FullTextSearchUpdater {

    void putFullTextOperationData(FullTextOperationData operationData);

}
