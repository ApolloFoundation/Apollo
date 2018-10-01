/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepositoryImpl;
import com.apollocurrency.aplwallet.apl.util.exception.InvalidTwoFactorAuthCredentialsException;
import com.apollocurrency.aplwallet.apl.util.exception.TwoFactoAuthAlreadyRegisteredException;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import org.junit.Assert;
import org.junit.Test;
import util.TwoFactorAuthUtil;

import java.security.GeneralSecurityException;
import java.util.Random;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.*;
import static org.mockito.Mockito.*;

public class TwoFactorAuthServiceIntegrationTest extends DbIntegrationTest {
    private TwoFactorAuthRepository repository = new TwoFactorAuthRepositoryImpl(db);
    private TwoFactorAuthService service = new TwoFactorAuthServiceImpl(repository);
    @Test
    public void testEnable() {
        TwoFactorAuthDetails authDetails = service.enable(ACCOUNT3.getAccount());
        TwoFactorAuthUtil.verifySecretCode(authDetails, ACCOUNT3.getAccountRS());
        Assert.assertFalse(service.isEnabled(ACCOUNT3.getAccount()));
    }

    @Test(expected = TwoFactoAuthAlreadyRegisteredException.class)
    public void testEnableAlreadyRegistered() {

        service.enable(ACCOUNT1.getAccount());
    }
    @Test
    public void testEnableNotConfirmed() {
        TwoFactorAuthDetails authDetails = service.enable(ACCOUNT2.getAccount());
        TwoFactorAuthUtil.verifySecretCode(authDetails, ACCOUNT2.getAccountRS());
        Assert.assertFalse(service.isEnabled(ACCOUNT2.getAccount()));
    }

    @Test
    public void testDisable() throws GeneralSecurityException {
        TwoFactorAuthService spy = spy(service);
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        spy.disable(ACCOUNT1.getAccount(), currentCode);
    }

    @Test(expected = InvalidTwoFactorAuthCredentialsException.class)
    public void testDisableFailAuth() {

        service.disable(ACCOUNT1.getAccount(), INVALID_CODE);
    }


    @Test
    public void testIsEnabledTrue() {

        boolean enabled = service.isEnabled(ACCOUNT1.getAccount());

        Assert.assertTrue(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenAccountIsNotExists() {

        boolean enabled = service.isEnabled(ACCOUNT3.getAccount());
        Assert.assertFalse(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenAccountIsNotConfirmed() {
        boolean enabled = service.isEnabled(ACCOUNT2.getAccount());
        Assert.assertFalse(enabled);
    }

    @Test
    public void testTryAuth() throws GeneralSecurityException {
        boolean authenticated = TwoFactorAuthUtil.tryAuth(service, ACCOUNT1.getAccount(), ACCOUNT1_2FA_SECRET_BASE32, MAX_2FA_ATTEMPTS);

        Assert.assertTrue(authenticated);
    }

    @Test
    public void testTryAuthCodesNotEquals() {
        int fakeNumber = new Random().nextInt();
        boolean authenticated = service.tryAuth(ACCOUNT1.getAccount(), fakeNumber);

        Assert.assertFalse(authenticated);
    }

    @Test
    public void testTryAuthNotFoundSecret() {
        int fakeNumber = new Random().nextInt();
        boolean authenticated = service.tryAuth(ACCOUNT2.getAccount(), fakeNumber);

        Assert.assertFalse(authenticated);
    }

    @Test
    public void testConfirm() throws GeneralSecurityException {
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT2_2FA_SECRET_BASE32);
        boolean confirm = service.confirm(ACCOUNT2.getAccount(), currentCode);
        Assert.assertTrue(confirm);
        Assert.assertTrue(service.isEnabled(ACCOUNT2.getAccount()));
    }
    @Test
    public void testConfirmAlreadyConfirmed() throws GeneralSecurityException {
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        boolean confirm = service.confirm(ACCOUNT1.getAccount(), currentCode);
        Assert.assertFalse(confirm);
        Assert.assertTrue(service.isEnabled(ACCOUNT1.getAccount()));
    }

    @Test
    public void testConfirmNotExists() throws GeneralSecurityException {
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT3_2FA_SECRET_BASE32);
        boolean confirm = service.confirm(ACCOUNT3.getAccount(), currentCode);
        Assert.assertFalse(confirm);
        Assert.assertFalse(service.isEnabled(ACCOUNT3.getAccount()));
    }
}
