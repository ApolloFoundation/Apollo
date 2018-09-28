/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.db;

import com.apollocurrency.aplwallet.apl.DbIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.*;


public class TwoFactorAuthRepositoryIntegrationTest extends DbIntegrationTest {
    private TwoFactorAuthRepository repository = new TwoFactorAuthRepositoryImpl(db);



    @Test
    public void testGet() {
        TwoFactorAuthEntity entity = repository.get(ACCOUNT1.getAccount());
        Assert.assertEquals(ENTITY1, entity);
    }

    @Test
    public void testGetNotFound() {

        TwoFactorAuthEntity entity = repository.get(ACCOUNT3.getAccount());
        Assert.assertNull(entity);
    }

    @Test
    public void testAdd() {
        boolean saved = repository.add(ENTITY3);
        Assert.assertTrue(saved);
        TwoFactorAuthEntity entity = repository.get(ACCOUNT3.getAccount());
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
        Assert.assertEquals(repository.get(ACCOUNT2.getAccount()), entity);
    }
    @Test
    public void testUpdateNotExist() {
        TwoFactorAuthEntity entity = new TwoFactorAuthEntity(ENTITY3.getAccount(), ENTITY3.getSecret(), false);
        boolean saved = repository.update(entity);
        Assert.assertFalse(saved);
    }

    @Test
    public void testDelete() {
        boolean deleted = repository.delete(ACCOUNT1.getAccount());
        Assert.assertTrue(deleted);
    }

    @Test
    public void testDeleteNothingToDelete() {
        boolean deleted = repository.delete(ACCOUNT3.getAccount());
        Assert.assertFalse(deleted);
    }
}
