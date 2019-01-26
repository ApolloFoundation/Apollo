/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT1;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT2;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT3;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ENTITY1;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ENTITY2;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ENTITY3;
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
        TwoFactorAuthEntity entity = repository.get(ACCOUNT1.getId());
        assertEquals(ENTITY1, entity);
    }

    @Test
    public void testGetNotFound() {

        TwoFactorAuthEntity entity = repository.get(ACCOUNT3.getId());
        assertNull(entity);
    }

    @Test
    public void testAdd() {
        boolean saved = repository.add(ENTITY3);
        assertTrue(saved);
        TwoFactorAuthEntity entity = repository.get(ACCOUNT3.getId());
        assertEquals(ENTITY3, entity);

    }

    @Test
    public void testAddAlreadyExist() {
        boolean saved = repository.add(ENTITY2);
        assertFalse(saved);
    }

    @Test
    public void testUpdate() {
        TwoFactorAuthEntity entity = new TwoFactorAuthEntity(ENTITY2.getAccount(), ENTITY2.getSecret(), false);
        boolean saved = repository.update(entity);
        assertTrue(saved);
        assertEquals(repository.get(ACCOUNT2.getId()), entity);
    }
    @Test
    public void testUpdateNotExist() {
        TwoFactorAuthEntity entity = new TwoFactorAuthEntity(ENTITY3.getAccount(), ENTITY3.getSecret(), false);
        boolean saved = repository.update(entity);
        assertFalse(saved);
    }

    @Test
    public void testDelete() {
        boolean deleted = repository.delete(ACCOUNT1.getId());
        assertTrue(deleted);
    }

    @Test
    public void testDeleteNothingToDelete() {
        boolean deleted = repository.delete(ACCOUNT3.getId());
        assertFalse(deleted);
    }
}
