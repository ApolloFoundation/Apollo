/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.cdi.transaction;

import org.jdbi.v3.core.CloseException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.transaction.TransactionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbiHandleFactoryTest {
    @Mock
    Jdbi jdbi;

    @Test
    void open_rollback_close_withException() {
        Handle handle = mock(Handle.class);
        when(jdbi.open()).thenReturn(handle);
        doThrow(new CloseException("Failed to clear transaction status on close", new SQLException("Connection is closed")))
            .when(handle).close();
        JdbiHandleFactory jdbiHandleFactory = new JdbiHandleFactory(this.jdbi);

        jdbiHandleFactory.open();
        assertTrue(jdbiHandleFactory.currentHandleOpened(), "Handle should be opened until closed");

        jdbiHandleFactory.rollback(); // exception thrown
        assertTrue(jdbiHandleFactory.currentHandleOpened(), "Handle should be opened until closed");

        jdbiHandleFactory.close(); // swallow CloseException and close handle
        assertFalse(jdbiHandleFactory.currentHandleOpened(), "Handle should be closed, even when CloseException occurs");
    }

    @Test
    void open_close_incorrectUsage() {
        Handle handle = mock(Handle.class);
        when(jdbi.open()).thenReturn(handle);
        doThrow(new TransactionException("Transaction is not committed")).when(handle).close();
        JdbiHandleFactory jdbiHandleFactory = new JdbiHandleFactory(this.jdbi);

        jdbiHandleFactory.open();
        assertTrue(jdbiHandleFactory.currentHandleOpened(), "Handle should be opened until closed");

        // assuming we perform many update sql statements

        // do not suppress TransactionException, because it's an indicator of the incorrect usage by programmers
        IllegalStateException ex = assertThrows(IllegalStateException.class, jdbiHandleFactory::close);
        assertEquals("Transaction is not finished before close", ex.getMessage());
        assertFalse(jdbiHandleFactory.currentHandleOpened(), "Handle should be closed, even when TransactionException occurs");
    }
}