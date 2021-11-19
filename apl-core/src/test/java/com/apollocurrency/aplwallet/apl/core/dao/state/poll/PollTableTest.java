package com.apollocurrency.aplwallet.apl.core.dao.state.poll;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Tag("slow")
class PollTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/poll.sql", "db/schema.sql");

    PollTable table;
    Event<FullTextOperationData> fullTextOperationDataEvent = mock(Event.class);

    @BeforeEach
    void setUp() {
        when(fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {})).thenReturn(fullTextOperationDataEvent);
        table = new PollTable(dbExtension.getDatabaseManager(), fullTextOperationDataEvent);
    }

    @Test
    void testRollback() {
        DbUtils.inTransaction(dbExtension, (con) -> table.rollback(1270));
        verify(fullTextOperationDataEvent, times(1)).select(new AnnotationLiteral<TrimEvent>() {});
    }

}