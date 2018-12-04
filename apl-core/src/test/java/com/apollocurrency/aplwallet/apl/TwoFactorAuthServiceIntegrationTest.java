/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT1;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT1_2FA_SECRET_BASE32;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT2;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT2_2FA_SECRET_BASE32;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT3;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT3_2FA_SECRET_BASE32;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.INVALID_CODE;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.MAX_2FA_ATTEMPTS;
import static org.mockito.Mockito.spy;

import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepositoryImpl;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import org.junit.Assert;
import org.junit.Test;
import util.TwoFactorAuthUtil;

import java.security.GeneralSecurityException;
import java.util.Random;

public class TwoFactorAuthServiceIntegrationTest extends DbIntegrationTest {
    private TwoFactorAuthRepository repository = new TwoFactorAuthRepositoryImpl(db);
    private TwoFactorAuthService service = new TwoFactorAuthServiceImpl(repository);
    @Test
    public void testEnable() {
        TwoFactorAuthDetails authDetails = service.enable(ACCOUNT3.getId());
        TwoFactorAuthUtil.verifySecretCode(authDetails, ACCOUNT3.getAccountRS());
        Assert.assertFalse(service.isEnabled(ACCOUNT3.getId()));
    }

    @Test
    public void testEnableAlreadyRegistered() {

        TwoFactorAuthDetails details2FA = service.enable(ACCOUNT1.getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.ALREADY_ENABLED, details2FA.getStatus2Fa());
    }
    @Test
    public void testEnableNotConfirmed() {
        TwoFactorAuthDetails authDetails = service.enable(ACCOUNT2.getId());
        TwoFactorAuthUtil.verifySecretCode(authDetails, ACCOUNT2.getAccountRS());
        Assert.assertFalse(service.isEnabled(ACCOUNT2.getId()));
    }

    @Test
    public void testDisable() throws GeneralSecurityException {
        TwoFactorAuthService spy = spy(service);
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        spy.disable(ACCOUNT1.getId(), currentCode);
    }

    @Test
    public void testDisableFailAuth() {

        TwoFactorAuthService.Status2FA status2FA = service.disable(ACCOUNT1.getId(), INVALID_CODE);

        Assert.assertEquals(TwoFactorAuthService.Status2FA.INCORRECT_CODE, status2FA);
    }


    @Test
    public void testIsEnabledTrue() {

        boolean enabled = service.isEnabled(ACCOUNT1.getId());

        Assert.assertTrue(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenAccountIsNotExists() {

        boolean enabled = service.isEnabled(ACCOUNT3.getId());
        Assert.assertFalse(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenAccountIsNotConfirmed() {
        boolean enabled = service.isEnabled(ACCOUNT2.getId());
        Assert.assertFalse(enabled);
    }

    @Test
    public void testTryAuth() throws GeneralSecurityException {
        boolean authenticated = TwoFactorAuthUtil.tryAuth(service, ACCOUNT1.getId(), ACCOUNT1_2FA_SECRET_BASE32, MAX_2FA_ATTEMPTS);

        Assert.assertTrue(authenticated);
    }

    @Test
    public void testTryAuthCodesNotEquals() {
        int fakeNumber = new Random().nextInt();
        TwoFactorAuthService.Status2FA status2FA = service.tryAuth(ACCOUNT1.getId(), fakeNumber);
        Assert.assertEquals(TwoFactorAuthService.Status2FA.INCORRECT_CODE, status2FA);
    }

    @Test
    public void testTryAuthNotConfirmed() {
        int fakeNumber = new Random().nextInt();
        TwoFactorAuthService.Status2FA status2FA = service.tryAuth(ACCOUNT2.getId(), fakeNumber);

        Assert.assertEquals(TwoFactorAuthService.Status2FA.NOT_CONFIRMED, status2FA);
    }

    @Test
    public void testConfirm() throws GeneralSecurityException {
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT2_2FA_SECRET_BASE32);
        TwoFactorAuthService.Status2FA status2FA = service.confirm(ACCOUNT2.getId(), currentCode);

        Assert.assertEquals(TwoFactorAuthService.Status2FA.OK, status2FA);
        Assert.assertTrue(service.isEnabled(ACCOUNT2.getId()));
    }
    @Test
    public void testConfirmAlreadyConfirmed() throws GeneralSecurityException {
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        TwoFactorAuthService.Status2FA status2FA = service.confirm(ACCOUNT1.getId(), currentCode);
        Assert.assertEquals(TwoFactorAuthService.Status2FA.ALREADY_CONFIRMED, status2FA);
        Assert.assertTrue(service.isEnabled(ACCOUNT1.getId()));
    }

    @Test
    public void testConfirmNotExists() throws GeneralSecurityException {
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT3_2FA_SECRET_BASE32);
        TwoFactorAuthService.Status2FA status2FA = service.confirm(ACCOUNT3.getId(), currentCode);
        Assert.assertEquals(TwoFactorAuthService.Status2FA.NOT_FOUND,status2FA);
        Assert.assertFalse(service.isEnabled(ACCOUNT3.getId()));
    }
}
