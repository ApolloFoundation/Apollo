/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public abstract class AbstractTwoFactorAuthRepositoryTest {
    protected TwoFactorAuthRepository repository;

    public AbstractTwoFactorAuthRepositoryTest(TwoFactorAuthRepository repository) {
        this.repository = repository;
    }

    public void setRepository(TwoFactorAuthRepository repository) {
        this.repository = repository;
    }

    protected AbstractTwoFactorAuthRepositoryTest() {
    }

    @Test
    public void testGet() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        TwoFactorAuthEntity entity = repository.get(td.ACCOUNT1.getId());
        assertEquals(td.ENTITY1, entity);
    }

    @Test
    public void testGetNotFound() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        TwoFactorAuthEntity entity = repository.get(td.ACCOUNT3.getId());
        assertNull(entity);
    }

    @Test
    public void testAdd() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();        
        boolean saved = repository.add(td.ENTITY3);
        assertTrue(saved);
        TwoFactorAuthEntity entity = repository.get(td.ACCOUNT3.getId());
        assertEquals(td.ENTITY3, entity);

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
        assertEquals(repository.get(td.ACCOUNT2.getId()), entity);
    }
    @Test
    public void testUpdateNotExist() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();                
        TwoFactorAuthEntity entity = new TwoFactorAuthEntity(td.ENTITY3.getAccount(), td.ENTITY3.getSecret(), false);
        boolean saved = repository.update(entity);
        assertFalse(saved);
    }

    @Test
    public void testDelete() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();                
        boolean deleted = repository.delete(td.ACCOUNT1.getId());
        assertTrue(deleted);
    }

    @Test
    public void testDeleteNothingToDelete() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();                        
        boolean deleted = repository.delete(td.ACCOUNT3.getId());
        assertFalse(deleted);
    }
}
