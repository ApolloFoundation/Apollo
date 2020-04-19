/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public abstract class AbstractTwoFactorAuthRepositoryTest {
    static {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        doReturn("APL").when(blockchainConfig).getAccountPrefix();
        Convert2.init(blockchainConfig);
    }

    protected TwoFactorAuthRepository repository;

    public AbstractTwoFactorAuthRepositoryTest(TwoFactorAuthRepository repository) {
        this.repository = repository;
    }

    protected AbstractTwoFactorAuthRepositoryTest() {
    }

    public void setRepository(TwoFactorAuthRepository repository) {
        this.repository = repository;
    }

    @Test
    public void testGet() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        TwoFactorAuthEntity entity = repository.get(td.ACC_1.getId());
        assertEquals(td.ENTITY1, entity);
    }

    @Test
    public void testGetNotFound() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        TwoFactorAuthEntity entity = repository.get(td.newAccount.getId());
        assertNull(entity);
    }

    @Test
    public void testAdd() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        boolean saved = repository.add(td.NEW_ENTITY);
        assertTrue(saved);
        TwoFactorAuthEntity entity = repository.get(td.newAccount.getId());
        assertEquals(td.NEW_ENTITY, entity);

    }

    @Test
    public void testAddAlreadyExist() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        boolean saved = repository.add(td.ENTITY2);
        assertFalse(saved);
    }

    @Test
    public void testUpdate() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        TwoFactorAuthEntity entity = new TwoFactorAuthEntity(td.ENTITY2.getAccount(), td.ENTITY2.getSecret(), false);
        boolean saved = repository.update(entity);
        assertTrue(saved);
        assertEquals(repository.get(td.ACC_2.getId()), entity);
    }

    @Test
    public void testUpdateNotExist() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        TwoFactorAuthEntity entity = new TwoFactorAuthEntity(td.NEW_ENTITY.getAccount(), td.NEW_ENTITY.getSecret(), false);
        boolean saved = repository.update(entity);
        assertFalse(saved);
    }

    @Test
    public void testDelete() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        boolean deleted = repository.delete(td.ACC_1.getId());
        assertTrue(deleted);
    }

    @Test
    public void testDeleteNothingToDelete() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        boolean deleted = repository.delete(td.newAccount.getId());
        assertFalse(deleted);
    }
}
