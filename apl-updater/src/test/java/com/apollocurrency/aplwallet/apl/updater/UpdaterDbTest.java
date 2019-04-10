/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.UpdaterMediatorImpl;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UpdateAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.repository.UpdaterDbRepository;
import com.apollocurrency.aplwallet.apl.updater.repository.UpdaterRepository;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.Platform;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

@Disabled
@ExtendWith(MockitoExtension.class)
public class UpdaterDbTest {
       private static final DbManipulator manipulator = new DbManipulator();
    protected static final DataSource dataSource = manipulator.getDataSource();
    private UpdaterRepository repository;
    @Mock
    private PropertiesHolder propertiesHolder;
    @Mock
    private DbProperties dbProperties;
    @Mock
    UpdaterMediator updaterMediator;

    @BeforeEach
    void setUp() {
        when(updaterMediator.getDataSource()).thenReturn(new TransactionalDataSource(dbProperties, propertiesHolder));
        repository = new UpdaterDbRepository(updaterMediator);
    }

    @Test
    public void testLoadUpdateTransaction() throws Exception {
        UpdateTransaction updateTransaction = repository.getLast();
        Transaction transaction = updateTransaction.getTransaction();
        assertEquals(Update.IMPORTANT, transaction.getType());
        assertEquals(104595, transaction.getHeight());
        assertEquals(((UpdateAttachment) transaction.getAttachment()).getAppVersion(), new Version("1.0.8"));
        assertEquals(((UpdateAttachment) transaction.getAttachment()).getArchitecture(), Architecture.X86);
        assertEquals(((UpdateAttachment) transaction.getAttachment()).getPlatform(), Platform.LINUX);
        assertEquals(Convert.toHexString(((UpdateAttachment) transaction.getAttachment()).getHash()), (
                "a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4"));
        assertTrue(updateTransaction.isUpdated());
    }

    @Test
    public void testClear() throws Exception {
        int actual = repository.clear();
        assertEquals(1, actual);
        UpdateTransaction tr = repository.getLast();
        assertNull(tr);
    }

    @Test
    public void testSaveUpdateTransaction() throws Exception {
        repository.clear();
        repository.save(new UpdateTransaction(-4081443370478530685L, false));
        UpdateTransaction updateTransaction = repository.getLast();
        Transaction transaction = updateTransaction.getTransaction();
        assertEquals(Update.CRITICAL, transaction.getType());
        assertEquals(104671, transaction.getHeight());
        assertEquals(((UpdateAttachment) transaction.getAttachment()).getAppVersion(), new Version("1.0.8"));
        assertEquals(((UpdateAttachment) transaction.getAttachment()).getArchitecture(), Architecture.AMD64);
        assertEquals(((UpdateAttachment) transaction.getAttachment()).getPlatform(), Platform.LINUX);
        assertEquals(Convert.toHexString(((UpdateAttachment) transaction.getAttachment()).getHash()), ("a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4"));
        assertFalse(updateTransaction.isUpdated());
    }

    @Test
    public void testClearAndSaveUpdateTransaction() {
        repository.clearAndSave(new UpdateTransaction(-4081443370478530685L, false));
        UpdateTransaction updateTransaction = repository.getLast();
        Transaction transaction = updateTransaction.getTransaction();
        assertEquals(-4081443370478530685L, transaction.getId());
        assertEquals(Update.CRITICAL, transaction.getAttachment().getTransactionType());
        assertFalse(updateTransaction.isUpdated());
    }

    @Test
    public void testSaveTwoUpdateTransactions() {
        repository.save(new UpdateTransaction(-4081443370478530685L, false));
        assertThrows(RuntimeException.class, () ->  {
            UpdateTransaction last = repository.getLast();
        });
    }

    private class MockUpdaterMediator extends UpdaterMediatorImpl {
        public MockUpdaterMediator() {
            super();
        }

        @Override
        public Transaction loadTransaction(Connection connection, ResultSet rs) throws AplException.NotValidException {
            try {
                int height = rs.getInt("height");
                long id = rs.getLong("id");
                byte type = rs.getByte("type");
                byte subType = rs.getByte("subtype");
                byte[] attachmentBytes = rs.getBytes("attachment_bytes");
                TransactionType transactionType = TransactionType.findTransactionType(type, subType);

                ByteBuffer buffer = ByteBuffer.wrap(attachmentBytes);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                UpdateAttachment attachment = (UpdateAttachment) transactionType.parseAttachment(buffer);
                SimpleTransaction simpleTransaction = new SimpleTransaction(id, transactionType, height);
                simpleTransaction.setAttachment(attachment);
                return simpleTransaction;
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public TransactionalDataSource getDataSource() {
            return new TransactionalDataSource(dbProperties, propertiesHolder) {
                Connection connection;

                @Override
                public Connection getConnection() throws SQLException {
                    connection = dataSource.getConnection();
                    return connection;
                }

                @Override
                public Connection begin() {
                    try {
                        connection = dataSource.getConnection();
                        connection.setAutoCommit(false);
                        return connection;
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }

                @Override
                public void rollback() {
                    if (connection != null) {
                        try {
                            connection.rollback();
                        }
                        catch (SQLException e) {
                            throw new RuntimeException(e.toString(), e);
                        }
                    }
                }

                @Override
                public void commit() {
                    try {
                        connection.commit();
                        connection.setAutoCommit(true);
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }
        };}



        }
    }
