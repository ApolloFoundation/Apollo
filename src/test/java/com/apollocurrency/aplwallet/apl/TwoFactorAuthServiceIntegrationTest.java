/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepositoryImpl;
import com.apollocurrency.aplwallet.apl.util.exception.InvalidTwoFactorAuthCredentialsException;
import com.apollocurrency.aplwallet.apl.util.exception.TwoFactoAuthAlreadyEnabledException;
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
        service.enable(ACCOUNT2.getAccount());

        Assert.assertTrue(service.isEnabled(ACCOUNT2.getAccount()));

    }

    @Test(expected = TwoFactoAuthAlreadyEnabledException.class)
    public void testEnableAlreadyEnabled() {

        service.enable(ACCOUNT1.getAccount());
    }

    @Test
    public void testDisable() throws GeneralSecurityException {
        TwoFactorAuthService spy = spy(service);
        long currentCode = TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        spy.disable(ACCOUNT1.getAccount(), currentCode);
        verify(spy, times(1)).tryAuth(ACCOUNT1.getAccount(), currentCode);

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
    public void testIsEnabledFalse() {

        boolean enabled = service.isEnabled(ACCOUNT2.getAccount());

        Assert.assertFalse(enabled);
    }

    @Test
    public void testTryAuth() throws GeneralSecurityException {
        boolean authenticated = TwoFactorAuthUtil.tryAuth(service);

        Assert.assertTrue(authenticated);
    }

    @Test
    public void testTryAuthCodesNotEquals() {
        long fakeNumber = new Random().nextLong();
        boolean authenticated = service.tryAuth(ACCOUNT1.getAccount(), fakeNumber);

        Assert.assertFalse(authenticated);
    }

    @Test
    public void testTryAuthNotFoundSecret() {
        long fakeNumber = new Random().nextLong();
        boolean authenticated = service.tryAuth(ACCOUNT2.getAccount(), fakeNumber);

        Assert.assertFalse(authenticated);
    }
}
