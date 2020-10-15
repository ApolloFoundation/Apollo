/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.cdi.transaction;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiTransactionalInterceptor;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.junit.AbstractWeldInitiator;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@Testcontainers
@Tag("slow")
@EnableWeld
public class JdbiInterceptorTest extends DbContainerBaseTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer);
    JdbiHandleFactory factory = spy(new JdbiHandleFactory());
    private Weld weld = AbstractWeldInitiator.createWeld();
    @WeldSetup
    public WeldInitiator weldInitiator = WeldInitiator.from(weld)
        .addBeans(MockBean.of(factory, JdbiHandleFactory.class)).build();
    @Inject
    private TransactionTestClass testClass;

    {
        factory.setJdbi(extension.getDatabaseManager().getJdbi());
        weld.addInterceptor(JdbiTransactionalInterceptor.class);

        weld.addBeanClasses(JdbiTransactionalInterceptor.class, TransactionTestClass.class, AnotherTransactionTestClass.class);
    }

    @Test
    void testOpenTransaction() {
        testClass.requireTransaction();
        verify(factory).open();
        verify(factory).begin();
        verify(factory).commit();
        verify(factory).close();
    }

    @Test
    void testOpenOneTransactionForMultipleInnerMethods() {
        testClass.requireOnlyOneTransaction();
        verify(factory, times(1)).isInTransaction();
        verify(factory).open();
        verify(factory).begin();
        verify(factory).commit();
        verify(factory).close();
    }

    @Test
    void testOpenOneTransactionForOuterMethodCall() {
        testClass.requireOnlyOneTransactionForOuterMethodCall();
        verify(factory, times(2)).isInTransaction();
        verify(factory).open();
        verify(factory).begin();
        verify(factory).commit();
        verify(factory).close();
    }

    @Test
    void testOpenReadOnlyTransaction() {
        testClass.requireReadOnlyTransaction();
        verify(factory).open();
        verify(factory, never()).begin();
        verify(factory, never()).commit();
        verify(factory).setReadOnly(true);
        verify(factory).close();
    }

    @Test
    void testOpenReadOnlyTransactionAndCallWriteTransaction() {
        doReturn(true).when(factory).isReadOnly(); // h2 doesnt support read only connections
        testClass.readOnlyCallNotReadonly();
        verify(factory, times(2)).isInTransaction();
        verify(factory).open();
        verify(factory).begin();
        verify(factory).commit();
        verify(factory).setReadOnly(true);
        verify(factory).close();
    }

    @Test
    void testOpenReadOnlyTransactionAndCallWriteTransactionForH2() {
        testClass.readOnlyCallNotReadonly();
        verify(factory, times(2)).isInTransaction();
        verify(factory).open();
        verify(factory).begin();
        verify(factory).commit();
        verify(factory).setReadOnly(true);
        verify(factory).close();
    }

    @Test
    void testOpenReadOnlyTransactionAndCallReadOnlyExternalMethod() {
        testClass.readOnlyCallReadOnlyFromAnotherClass();
        verify(factory, times(2)).isInTransaction();
        verify(factory).open();
        verify(factory).close();
        verify(factory).setReadOnly(true);
        verify(factory, never()).rollback();
        verify(factory, never()).begin();
        verify(factory, never()).commit();
    }

    @Test
    void testThrowExceptionOnNotReadonlyTransaction() {
        doThrow(new RuntimeException("Test exception")).when(factory).begin();
        assertThrows(RuntimeException.class, () -> testClass.requireTransaction());
        verify(factory).isInTransaction();
        verify(factory).open();
        verify(factory).close();
        verify(factory).rollback();
        verify(factory).begin();
        verify(factory, never()).commit();
    }

    @Test
    void testThrowExceptionOnReadOnlyTransaction() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(
            "com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiTransactionalInterceptor");
        logger.setLevel(Level.TRACE);
        ;
        doThrow(new RuntimeException("Test exception")).when(factory).setReadOnly(true);
        assertThrows(RuntimeException.class, () -> testClass.requireReadOnlyTransaction());
        verify(factory).isInTransaction();
        verify(factory).open();
        verify(factory).close();
        verify(factory, never()).rollback();
        verify(factory, never()).begin();
        verify(factory, never()).commit();
    }

    @Singleton
    public static class TransactionTestClass {
        private static final Logger LOG = LoggerFactory.getLogger(TransactionTestClass.class);

        @Transactional
        void requireTransaction() {
            LOG.debug("dfs");
        }

        @Transactional(readOnly = true)
        void readOnlyCallNotReadonly() {
            CDI.current().select(AnotherTransactionTestClass.class).get().requireTransaction();
        }

        @Transactional(readOnly = true)
        void requireReadOnlyTransaction() {
        }

        @Transactional(readOnly = true)
        void readOnlyCallReadOnlyFromAnotherClass() {
            CDI.current().select(AnotherTransactionTestClass.class).get().requireReadOnlyTransaction();
        }

        @Transactional
        void requireOnlyOneTransaction() {
            requireTransaction();
        }

        @Transactional
        void requireOnlyOneTransactionForOuterMethodCall() {
            CDI.current().select(AnotherTransactionTestClass.class).get().requireTransaction();
        }
    }

    @Singleton
    public static class AnotherTransactionTestClass {
        private static final Logger LOG = LoggerFactory.getLogger(TransactionTestClass.class);

        @Transactional
        void requireTransaction() {
            LOG.debug("dfs");
        }

        @Transactional(readOnly = true)
        void requireReadOnlyTransaction() {
        }

    }
}
