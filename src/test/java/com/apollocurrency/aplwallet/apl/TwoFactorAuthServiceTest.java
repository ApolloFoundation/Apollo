/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT1;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT1_2FA_SECRET_BASE32;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT2;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT2_2FA_SECRET_BASE32;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ENTITY1;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ENTITY2;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ENTITY3;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.INVALID_CODE;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.MAX_2FA_ATTEMPTS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.security.GeneralSecurityException;
import java.util.Random;

import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthEntity;
import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.TwoFactorAuthUtil;

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
        TwoFactorAuthUtil.verifySecretCode(twoFactorAuthDetails, Convert.defaultRsAccount(ENTITY3.getAccount()));
        Assert.assertEquals(TwoFactorAuthService.Status2FA.OK, twoFactorAuthDetails.getStatus2Fa());
        verify(repository, times(1)).add(any(TwoFactorAuthEntity.class));
    }

    @Test
    public void testEnableAlreadyEnabled() {
        doReturn(ENTITY1).when(repository).get(ACCOUNT1.getId());

        TwoFactorAuthDetails details = service.enable(ACCOUNT1.getId());

        Assert.assertEquals(TwoFactorAuthService.Status2FA.ALREADY_ENABLED, details.getStatus2Fa());
    }
    @Test
    public void testEnableNotConfirmed() {
        doReturn(ENTITY2).when(repository).get(ACCOUNT2.getId());

        TwoFactorAuthDetails details = service.enable(ACCOUNT2.getId());
        TwoFactorAuthUtil.verifySecretCode(details, Convert.defaultRsAccount(ACCOUNT2.getId()));
        Assert.assertEquals(ACCOUNT2_2FA_SECRET_BASE32, details.getSecret());
    }


    @Test
    public void testDisable() throws GeneralSecurityException {
        TwoFactorAuthService spy = spy(service);
        int code = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        doReturn(ENTITY1).when(repository).get(ACCOUNT1.getId());

        spy.disable(ACCOUNT1.getId(), code);

        verify(repository, times(1)).get(ACCOUNT1.getId());
        verify(repository, times(1)).delete(ACCOUNT1.getId());
    }

    @Test
    public void testDisableFailAuth() {
        TwoFactorAuthService spy = spy(service);
        doReturn(TwoFactorAuthService.Status2FA.INCORRECT_CODE).when(spy).tryAuth(ACCOUNT1.getId(), INVALID_CODE);
        doReturn(ENTITY1).when(repository).get(ACCOUNT1.getId());

        TwoFactorAuthService.Status2FA status2FA = spy.disable(ACCOUNT1.getId(), INVALID_CODE);
        Assert.assertEquals(TwoFactorAuthService.Status2FA.INCORRECT_CODE, status2FA);
    }


    @Test
    public void testIsEnabledTrue() {
        doReturn(ENTITY1).when(repository).get(ACCOUNT1.getId());

        boolean enabled = service.isEnabled(ACCOUNT1.getId());

        verify(repository, times(1)).get(ACCOUNT1.getId());

        Assert.assertTrue(enabled);
    }

    @Test
    public void testIsEnabledFalse() {

        boolean enabled = service.isEnabled(ACCOUNT1.getId());

        verify(repository, times(1)).get(ACCOUNT1.getId());

        Assert.assertFalse(enabled);
    }
    @Test
    public void testIsEnabledFalseWhen2faWasNotConfirmed() throws CloneNotSupportedException {
        doReturn(ENTITY2.clone()).when(repository).get(ACCOUNT2.getId());

        boolean enabled = service.isEnabled(ACCOUNT2.getId());

        verify(repository, times(1)).get(ACCOUNT2.getId());

        Assert.assertFalse(enabled);
    }


    @Test
    public void testTryAuth() throws GeneralSecurityException {
        doReturn(ENTITY1).when(repository).get(ACCOUNT1.getId());

        boolean authenticated = TwoFactorAuthUtil.tryAuth(service, ACCOUNT1.getId(), ACCOUNT1_2FA_SECRET_BASE32, MAX_2FA_ATTEMPTS);
        verify(repository, atMost(MAX_2FA_ATTEMPTS)).get(ACCOUNT1.getId());

        Assert.assertTrue(authenticated);
    }

    @Test
    public void testTryAuthCodesNotEquals() throws GeneralSecurityException {
        doReturn(ENTITY1).when(repository).get(ACCOUNT1.getId());

        int currentNumber = new Random().nextInt();
        TwoFactorAuthService.Status2FA status2FA = service.tryAuth(ACCOUNT1.getId(), currentNumber);

        verify(repository, times(1)).get(ACCOUNT1.getId());

        Assert.assertEquals(TwoFactorAuthService.Status2FA.INCORRECT_CODE, status2FA);
    }

    @Test
    public void testTryAuthNotFoundSecretForAccount() throws GeneralSecurityException {
//        just to make sure that 2fa code is correct
        int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        TwoFactorAuthService.Status2FA status2FA = service.tryAuth(ACCOUNT1.getId(), currentNumber);

        verify(repository, times(1)).get(ACCOUNT1.getId());

        Assert.assertEquals(TwoFactorAuthService.Status2FA.NOT_ENABLED, status2FA);
    }
    @Test
    public void testTryAuthForNotConfirmedAccount() throws GeneralSecurityException, CloneNotSupportedException {
        doReturn(ENTITY2.clone()).when(repository).get(ACCOUNT2.getId());
        int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT2_2FA_SECRET_BASE32);
        TwoFactorAuthService.Status2FA status2FA = service.tryAuth(ACCOUNT2.getId(), currentNumber);

        verify(repository, times(1)).get(ACCOUNT2.getId());

        Assert.assertEquals(TwoFactorAuthService.Status2FA.NOT_CONFIRMED, status2FA);
    }
    @Test
    public void testConfirm() throws GeneralSecurityException, CloneNotSupportedException {
        TwoFactorAuthEntity clone = ENTITY2.clone();
        doReturn(clone).when(repository).get(ACCOUNT2.getId());
        doReturn(true).when(repository).update(clone);

        int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT2_2FA_SECRET_BASE32);
        TwoFactorAuthService.Status2FA status2FA = service.confirm(ACCOUNT2.getId(), currentNumber);

        verify(repository, times(1)).get(ACCOUNT2.getId());
        verify(repository, times(1)).update(clone);

        Assert.assertEquals(TwoFactorAuthService.Status2FA.OK, status2FA);
    }

    @Test
    public void testConfirmForAlreadyEnabledAccount() throws GeneralSecurityException, CloneNotSupportedException {
        doReturn(ENTITY1.clone()).when(repository).get(ACCOUNT1.getId());
        int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        TwoFactorAuthService.Status2FA status2FA = service.confirm(ACCOUNT1.getId(), currentNumber);

        verify(repository, times(1)).get(ACCOUNT1.getId());

        Assert.assertEquals(TwoFactorAuthService.Status2FA.ALREADY_CONFIRMED, status2FA);
    }
}
