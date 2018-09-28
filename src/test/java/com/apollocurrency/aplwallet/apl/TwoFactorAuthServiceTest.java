/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthEntity;
import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.InvalidTwoFactorAuthCredentialsException;
import com.apollocurrency.aplwallet.apl.util.exception.TwoFactoAuthAlreadyRegisteredException;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.TwoFactorAuthUtil;

import java.security.GeneralSecurityException;
import java.util.Random;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.*;
import static org.mockito.Matchers.any;
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
        doReturn(true).when(repository).add(any(TwoFactorAuthEntity.class));
        TwoFactorAuthDetails twoFactorAuthDetails = service.enable(ENTITY3.getAccount());
        TwoFactorAuthUtil.verifySecretCode(twoFactorAuthDetails, Convert.rsAccount(ENTITY3.getAccount()));

        verify(repository, times(1)).add(any(TwoFactorAuthEntity.class));
    }

    @Test(expected = TwoFactoAuthAlreadyRegisteredException.class)
    public void testEnableAlreadyEnabled() {
        doReturn(false).when(repository).add(any(TwoFactorAuthEntity.class));

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

    @Test(expected = InvalidTwoFactorAuthCredentialsException.class)
    public void testDisableFailAuth() {
        TwoFactorAuthService spy = spy(service);
        doReturn(false).when(spy).tryAuth(ACCOUNT1.getAccount(), INVALID_CODE);

        spy.disable(ACCOUNT1.getAccount(), INVALID_CODE);
    }


    @Test
    public void testIsEnabledTrue() {
        doReturn(ENTITY1).when(repository).get(ACCOUNT1.getAccount());

        boolean enabled = service.isEnabled(ACCOUNT1.getAccount());

        verify(repository, times(1)).get(ACCOUNT1.getAccount());

        Assert.assertTrue(enabled);
    }

    @Test
    public void testIsEnabledFalse() {

        boolean enabled = service.isEnabled(ACCOUNT1.getAccount());

        verify(repository, times(1)).get(ACCOUNT1.getAccount());

        Assert.assertFalse(enabled);
    }
    @Test
    public void testIsEnabledFalseWhen2faWasNotConfirmed() throws CloneNotSupportedException {
        doReturn(ENTITY2.clone()).when(repository).get(ACCOUNT2.getAccount());

        boolean enabled = service.isEnabled(ACCOUNT2.getAccount());

        verify(repository, times(1)).get(ACCOUNT2.getAccount());

        Assert.assertFalse(enabled);
    }


    @Test
    public void testTryAuth() throws GeneralSecurityException {
        doReturn(ENTITY1).when(repository).get(ACCOUNT1.getAccount());

        boolean authenticated = TwoFactorAuthUtil.tryAuth(service, ACCOUNT1.getAccount(), ACCOUNT1_2FA_SECRET_BASE32, MAX_2FA_ATTEMPTS);
        verify(repository, atMost(MAX_2FA_ATTEMPTS)).get(ACCOUNT1.getAccount());

        Assert.assertTrue(authenticated);
    }

    @Test
    public void testTryAuthCodesNotEquals() throws GeneralSecurityException {
        doReturn(ENTITY1).when(repository).get(ACCOUNT1.getAccount());

        int currentNumber = new Random().nextInt();
        boolean authenticated = service.tryAuth(ACCOUNT1.getAccount(), currentNumber);

        verify(repository, times(1)).get(ACCOUNT1.getAccount());

        Assert.assertFalse(authenticated);
    }

    @Test
    public void testTryAuthNotFoundSecretForAccount() throws GeneralSecurityException {
//        just to make sure that 2fa code is correct
        int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        boolean authenticated = service.tryAuth(ACCOUNT1.getAccount(), currentNumber);

        verify(repository, times(1)).get(ACCOUNT1.getAccount());

        Assert.assertFalse(authenticated);
    }
    @Test
    public void testTryAuthForNotConfirmedAccount() throws GeneralSecurityException, CloneNotSupportedException {
        doReturn(ENTITY2.clone()).when(repository).get(ACCOUNT2.getAccount());
        int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT2_2FA_SECRET_BASE32);
        boolean authenticated = service.tryAuth(ACCOUNT2.getAccount(), currentNumber);

        verify(repository, times(1)).get(ACCOUNT2.getAccount());

        Assert.assertFalse(authenticated);
    }
    @Test
    public void testConfirmEnabling() throws GeneralSecurityException, CloneNotSupportedException {
        doReturn(ENTITY2.clone()).when(repository).get(ACCOUNT2.getAccount());
        int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT2_2FA_SECRET_BASE32);
        boolean confirmed = service.confirmEnabling(ACCOUNT2.getAccount(), currentNumber);

        verify(repository, times(1)).get(ACCOUNT2.getAccount());

        Assert.assertTrue(confirmed);
    }

    @Test
    public void testConfirmEnablingForAlreadyEnabledAccount() throws GeneralSecurityException, CloneNotSupportedException {
        doReturn(ENTITY1.clone()).when(repository).get(ACCOUNT1.getAccount());
        int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        boolean confirmed = service.confirmEnabling(ACCOUNT1.getAccount(), currentNumber);

        verify(repository, times(1)).get(ACCOUNT1.getAccount());

        Assert.assertFalse(confirmed);
    }
}
