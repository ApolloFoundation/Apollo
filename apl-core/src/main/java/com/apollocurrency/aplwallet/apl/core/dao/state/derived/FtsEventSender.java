/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FtsEventSender {

    private final Event<FullTextOperationData> fullTextOperationDataEvent;

    public FtsEventSender(Event<FullTextOperationData> fullTextOperationDataEvent) {
        this.fullTextOperationDataEvent = fullTextOperationDataEvent;
    }

    public Integer fireEventsForDeletedIds(
        FullTextOperationData operationData,
        ResultSet deletedIds,
        int height,
        Integer deletedRecordsCount) throws SQLException {
        // take one DB_ID and fire Event to FTS with data
        while (deletedIds.next()) {
            Long deleted_db_id = deletedIds.getLong("DB_ID");
            operationData.setDbIdValue(deleted_db_id);
            // fire event to update FullTextSearch index for record deletion
            fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {})
                .fire(operationData);
            ++deletedRecordsCount;
            log.trace("Update lucene index for '{}' at height = {}, deletedRecordsCount = {} by data :\n{}",
                operationData.getTableName(), height, deletedRecordsCount, operationData);
        }
        return deletedRecordsCount;
    }

}
