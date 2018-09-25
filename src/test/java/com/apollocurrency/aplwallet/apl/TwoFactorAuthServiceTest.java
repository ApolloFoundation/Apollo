/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepository;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.GeneralSecurityException;
import java.util.Random;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TwoFactorAuthServiceTest {
    @Mock
    private TwoFactorAuthRepository repository;
    private TwoFactorAuthService service;

    @Before
    public void setUp() throws Exception {
        service = new TwoFactorAuthServiceImpl(repository);
    }

    @Test
    public void testEnable() {
        service.enable(ACCOUNT2.getAccount());
        verify(repository, times(1)).saveSecret(anyLong(), any(byte[].class));
    }

    @Test(expected = RuntimeException.class)
    public void testEnableAlreadyEnabled() {
        doThrow(new AlreadyExistsException()).when(repository).saveSecret(anyLong(), any(byte[].class));

        service.enable(ACCOUNT2.getAccount());
    }

    @Test
    public void testDisable() {
        TwoFactorAuthService spy = spy(service);
        doReturn(true).when(spy).tryAuth(ACCOUNT1.getAccount(), INVALID_CODE);

        spy.disable(ACCOUNT1.getAccount(), INVALID_CODE);

        verify(spy, times(1)).tryAuth(ACCOUNT1.getAccount(), INVALID_CODE);
        verify(repository, times(1)).delete(ACCOUNT1.getAccount());
    }

    @Test(expected = RuntimeException.class)
    public void testDisableFailAuth() {
        TwoFactorAuthService spy = spy(service);
        doReturn(false).when(spy).tryAuth(ACCOUNT1.getAccount(), INVALID_CODE);

        spy.disable(ACCOUNT1.getAccount(), INVALID_CODE);
    }

    @Test(expected = RuntimeException.class)
    public void testDisableWhenAccountHasNot2FA() {

        TwoFactorAuthService spy = spy(service);
        doReturn(true).when(spy).tryAuth(ACCOUNT1.getAccount(), INVALID_CODE);
        doThrow(new NotFoundException("Account has not 2fa")).when(repository).delete(ACCOUNT1.getAccount());
        spy.disable(ACCOUNT1.getAccount(), INVALID_CODE);

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
