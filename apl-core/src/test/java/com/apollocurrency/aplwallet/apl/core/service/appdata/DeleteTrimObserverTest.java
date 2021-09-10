/*
 *  Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.service.appdata;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@EnableWeld
class DeleteTrimObserverTest {
    DatabaseManager databaseManager = mock(DatabaseManager.class);
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(DeleteTrimObserver.class)
        .addBeans(MockBean.of(databaseManager, DatabaseManager.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .build();

    @Inject
    Event<DeleteOnTrimData> trimEvent;
    @Inject
    DeleteTrimObserver observer;
    @Inject
    Event<FullTextOperationData> fullTextOperationDataEvent;

    @BeforeEach
    void setUp() {
        doReturn(10).when(propertiesHolder).BATCH_COMMIT_SIZE();
    }

    @Test
    void sendResetEvent() {
        trimEvent.select(new AnnotationLiteral<TrimEvent>() {})
            .fireAsync(new DeleteOnTrimData(true, Collections.emptySet(), "null"));
        assertEquals(0, observer.getDeleteOnTrimDataQueue().size());
    }

    @Test
    @Timeout(value = 20)
    void sendDeleteEvent() {
        assertNotNull(observer);
        trimEvent.select(new AnnotationLiteral<TrimEvent>() {})
            .fireAsync(new DeleteOnTrimData(false, Collections.emptySet(), "some_table"));
        while (true) {
            int size = observer.getDeleteOnTrimDataQueueSize();
            if (size == 1) {
                break;
            }
            ThreadUtils.sleep(100);
        }
    }

    @Test
    void doNotPerformTrimOnIncorrectData() {
        long result = observer.performOneTableDelete(null);
        assertEquals(0, result);

        DeleteOnTrimData set1 = new DeleteOnTrimData(true, null, "null");
        result = observer.performOneTableDelete(set1);
        assertEquals(0, result);

        DeleteOnTrimData set2 = new DeleteOnTrimData(true, Collections.emptySet(), "null");
        result = observer.performOneTableDelete(set2);
        assertEquals(0, result);
    }

    @Test
    void performDelete() throws Exception {
        TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        doReturn(true).when(dataSource).isInTransaction();
        doNothing().when(dataSource).commit(false);
        doReturn(dataSource).when(databaseManager).getDataSource();

        Connection con = mock(Connection.class);
        doReturn(con).when(dataSource).getConnection();
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        doReturn(preparedStatement).when(con).prepareStatement(anyString());
        doNothing().doNothing().when(preparedStatement).setLong(anyInt(), anyLong());
        observer = new DeleteTrimObserver(databaseManager, propertiesHolder, fullTextOperationDataEvent);

        DeleteOnTrimData delete = new DeleteOnTrimData(true, Set.of(1739068987193023818L, 9211698109297098287L), "account");
        observer.performOneTableDelete(delete);

        verify(dataSource).commit(false);
        verify(preparedStatement, times(2)).executeUpdate();
    }
}