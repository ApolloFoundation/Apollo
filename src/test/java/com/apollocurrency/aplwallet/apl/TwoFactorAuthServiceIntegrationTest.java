/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepositoryImpl;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import org.apache.commons.codec.binary.Base32;
import org.junit.Assert;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.Random;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.*;
import static org.mockito.Mockito.*;

public class TwoFactorAuthServiceIntegrationTest extends DbIntegrationTest {
    private TwoFactorAuthRepository repository = new TwoFactorAuthRepositoryImpl(db);
    private TwoFactorAuthService service = new TwoFactorAuthServiceImpl(repository);
    private static final Base32 BASE32 = new Base32();
    @Test
    public void testEnable() {
        service.enable(ACCOUNT2.getAccount());

        Assert.assertTrue(service.isEnabled(ACCOUNT2.getAccount()));

    }

    @Test(expected = RuntimeException.class)
    public void testEnableAlreadyEnabled() {

        service.enable(ACCOUNT1.getAccount());
    }

    @Test
    public void testDisable() throws GeneralSecurityException {

        long currentCode = TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        TwoFactorAuthService spy = spy(service);
        spy.disable(ACCOUNT1.getAccount(), currentCode);
        verify(spy, times(1)).tryAuth(ACCOUNT1.getAccount(), currentCode);

    }

    @Test(expected = RuntimeException.class)
    public void testDisableFailAuth() {

        service.disable(ACCOUNT1.getAccount(), INVALID_CODE);
    }


    @Test
    public void testIsEnabledTrue() {
        doReturn(ACCOUNT1_2FA_SECRET_BYTES).when(repository).getSecret(ACCOUNT1.getAccount());

        boolean enabled = service.isEnabled(ACCOUNT1.getAccount());

        verify(repository, times(1)).getSecret(ACCOUNT1.getAccount());

        Assert.assertTrue(enabled);
    }

    @Test
    public void testIsEnabledFalse() {

        boolean enabled = service.isEnabled(ACCOUNT1.getAccount());

        verify(repository, times(1)).getSecret(ACCOUNT1.getAccount());

        Assert.assertFalse(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenNotFoundException() {
        doThrow(new NotFoundException("Not found 2fa for account")).when(repository).getSecret(ACCOUNT1.getAccount());

        boolean enabled = service.isEnabled(ACCOUNT1.getAccount());

        verify(repository, times(1)).getSecret(ACCOUNT1.getAccount());

        Assert.assertFalse(enabled);
    }

    @Test
    public void testTryAuth() throws GeneralSecurityException {
        doReturn(ACCOUNT1_2FA_SECRET_BYTES).when(repository).getSecret(ACCOUNT1.getAccount());

        long currentNumber = TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        boolean authenticated = service.tryAuth(ACCOUNT1.getAccount(), currentNumber);

        verify(repository, times(1)).getSecret(ACCOUNT1.getAccount());

        Assert.assertTrue(authenticated);
    }

    @Test
    public void testTryAuthCodesNotEquals() throws GeneralSecurityException {
        doReturn(ACCOUNT1_2FA_SECRET_BYTES).when(repository).getSecret(ACCOUNT1.getAccount());

        long currentNumber = new Random().nextLong();
        boolean authenticated = service.tryAuth(ACCOUNT1.getAccount(), currentNumber);

        verify(repository, times(1)).getSecret(ACCOUNT1.getAccount());

        Assert.assertFalse(authenticated);
    }

    @Test
    public void testTryAuthNotFoundException() throws GeneralSecurityException {
        doThrow(new NotFoundException("Not found 2fa for account")).when(repository).getSecret(ACCOUNT1.getAccount());

        long currentNumber = TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        boolean authenticated = service.tryAuth(ACCOUNT1.getAccount(), currentNumber);

        verify(repository, times(1)).getSecret(ACCOUNT1.getAccount());

        Assert.assertFalse(authenticated);
    }
}
