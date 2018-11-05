/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

import java.sql.Connection;

import com.apollocurrency.aplwallet.apl.db.TransactionalDb;
import com.apollocurrency.aplwallet.apl.updater.Architecture;
import com.apollocurrency.aplwallet.apl.updater.Platform;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Db.class, TransactionalDb.class, Constants.class, Apl.class, BlockchainImpl.class})
@SuppressStaticInitializationFor({"com.apollocurrency.aplwallet.apl.Db","com.apollocurrency.aplwallet.apl.util.Logger", "com.apollocurrency.aplwallet.apl.db.TransactionalDb","com.apollocurrency" +
        ".aplwallet.apl.Constants", "com" +
        ".apollocurrency" +
        ".aplwallet.apl.Apl"})
public class UpdaterDbTest {
    private EmbeddedDatabase db;

    @Before
    public void setUp() throws Exception {
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("db/schema.sql")
                .addScript("db/data.sql")
                .build();
        mockStatic(Apl.class);
        mockStatic(Db.class);
        mockStatic(TransactionalDb.class);
        mockStatic(Constants.class);
        TransactionalDb fakeDb = mock(TransactionalDb.class);
        PowerMockito.doReturn(fakeDb).when(Db.class, "getDb");
        Whitebox.setInternalState(Constants.class, "correctInvalidFees", false);
        BlockchainImpl mock = spy(BlockchainImpl.getInstance());
        PowerMockito.doReturn(100).when(mock).getHeight();
        spy(Apl.class);
        PowerMockito.doReturn(mock).when(Apl.class, "getBlockchain");
        when(fakeDb.getConnection()).then(invocationOnMock -> db.getConnection());
        Connection connection = db.getConnection();
        when(fakeDb.beginTransaction()).then(invocationOnMock -> {
            connection.setAutoCommit(false);
            return connection;
        });
        doAnswer(invocation -> {
            connection.setAutoCommit(true);
            connection.commit();
            return null;
        }).when(fakeDb).commitTransaction();
    }

    @After
    public void tearDown() throws Exception {
        db.shutdown();
    }

    @Test
    public void testLoadUpdateTransaction() throws Exception {
        Transaction transaction = UpdaterDb.loadLastUpdateTransaction();
        Assert.assertEquals(TransactionType.Update.IMPORTANT, transaction.getType());
        Assert.assertEquals(104595, transaction.getHeight());
        Assert.assertEquals(((Attachment.UpdateAttachment) transaction.getAttachment()).getAppVersion(), Version.from("1.0.8"));
        Assert.assertEquals(((Attachment.UpdateAttachment) transaction.getAttachment()).getArchitecture(), Architecture.X86);
        Assert.assertEquals(((Attachment.UpdateAttachment) transaction.getAttachment()).getPlatform(), Platform.LINUX);
        Assert.assertEquals(Convert.toHexString(((Attachment.UpdateAttachment) transaction.getAttachment()).getHash()), ("a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4"));
    }
    @Test
    public void testClear() throws Exception {
        int actual = UpdaterDb.clear();
        Assert.assertEquals(1, actual);
        Transaction tr = UpdaterDb.loadLastUpdateTransaction();
        Assert.assertNull(tr);
    }
    @Test
    public void testSaveUpdateTransaction() throws Exception {
        UpdaterDb.clear();
        boolean saved = UpdaterDb.saveUpdateTransaction(-4081443370478530685L);
        Assert.assertTrue(saved);
        Transaction transaction = UpdaterDb.loadLastUpdateTransaction();
        Assert.assertEquals(TransactionType.Update.CRITICAL, transaction.getType());
        Assert.assertEquals(104671, transaction.getHeight());
        Assert.assertEquals(((Attachment.UpdateAttachment) transaction.getAttachment()).getAppVersion(), Version.from("1.0.8"));
        Assert.assertEquals(((Attachment.UpdateAttachment) transaction.getAttachment()).getArchitecture(), Architecture.AMD64);
        Assert.assertEquals(((Attachment.UpdateAttachment) transaction.getAttachment()).getPlatform(), Platform.LINUX);
        Assert.assertEquals(Convert.toHexString(((Attachment.UpdateAttachment) transaction.getAttachment()).getHash()), ("a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4"));
    }

    @Test
    public void testGetUpdateStatus() {
        boolean updateStatus = UpdaterDb.getUpdateStatus();
        Assert.assertTrue(updateStatus);
        UpdaterDb.clearAndSaveUpdateTransaction(-4081443370478530685L);
        Assert.assertFalse(UpdaterDb.getUpdateStatus());
    }

    @Test
    public void testSaveUpdateStatus() {
        UpdaterDb.saveUpdateStatus(false);
        Assert.assertFalse(UpdaterDb.getUpdateStatus());
        UpdaterDb.saveUpdateStatus(true);
        Assert.assertTrue(UpdaterDb.getUpdateStatus());
    }

    @Test
    public void testClearAndSaveUpdateTransaction() {
        boolean saved = UpdaterDb.clearAndSaveUpdateTransaction(-4081443370478530685L);
        Assert.assertTrue(saved);
        Transaction transaction = UpdaterDb.loadLastUpdateTransaction();
        Assert.assertEquals(-4081443370478530685L, transaction.getId());
        Assert.assertEquals(TransactionType.Update.CRITICAL, transaction.getAttachment().getTransactionType());
        Assert.assertFalse(UpdaterDb.getUpdateStatus());
    }


}
