/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT1;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT2;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT3;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ENTITY1;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ENTITY2;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ENTITY3;

import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthEntity;
import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepository;
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals(ENTITY1, entity);
    }

    @Test
    public void testGetNotFound() {

        TwoFactorAuthEntity entity = repository.get(ACCOUNT3.getId());
        Assert.assertNull(entity);
    }

    @Test
    public void testAdd() {
        boolean saved = repository.add(ENTITY3);
        Assert.assertTrue(saved);
        TwoFactorAuthEntity entity = repository.get(ACCOUNT3.getId());
        Assert.assertEquals(ENTITY3, entity);

    }

    @Test
    public void testAddAlreadyExist() {
        boolean saved = repository.add(ENTITY2);
        Assert.assertFalse(saved);
    }

    @Test
    public void testUpdate() {
        TwoFactorAuthEntity entity = new TwoFactorAuthEntity(ENTITY2.getAccount(), ENTITY2.getSecret(), false);
        boolean saved = repository.update(entity);
        Assert.assertTrue(saved);
        Assert.assertEquals(repository.get(ACCOUNT2.getId()), entity);
    }
    @Test
    public void testUpdateNotExist() {
        TwoFactorAuthEntity entity = new TwoFactorAuthEntity(ENTITY3.getAccount(), ENTITY3.getSecret(), false);
        boolean saved = repository.update(entity);
        Assert.assertFalse(saved);
    }

    @Test
    public void testDelete() {
        boolean deleted = repository.delete(ACCOUNT1.getId());
        Assert.assertTrue(deleted);
    }

    @Test
    public void testDeleteNothingToDelete() {
        boolean deleted = repository.delete(ACCOUNT3.getId());
        Assert.assertFalse(deleted);
    }
}
