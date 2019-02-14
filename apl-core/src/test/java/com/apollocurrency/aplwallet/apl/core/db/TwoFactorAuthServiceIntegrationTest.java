/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT1;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT1_2FA_SECRET_BASE32;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT2;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT2_2FA_SECRET_BASE32;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT3;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT3_2FA_SECRET_BASE32;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.INVALID_CODE;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.MAX_2FA_ATTEMPTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthService;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthServiceImpl;
import com.apollocurrency.aplwallet.apl.testutil.TwoFactorAuthUtil;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.Random;

@EnableWeld
public class TwoFactorAuthServiceIntegrationTest extends DbTest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            NtpTime.class
    ).build();

    private TwoFactorAuthRepository repository = new TwoFactorAuthRepositoryImpl(getDataSource());
    private TwoFactorAuthService service = new TwoFactorAuthServiceImpl(repository, "test");

    @Test
    public void testEnable() {
        TwoFactorAuthDetails authDetails = service.enable(ACCOUNT3.getId());
        TwoFactorAuthUtil.verifySecretCode(authDetails, Convert2.defaultRsAccount(ACCOUNT3.getId()));
        assertFalse(service.isEnabled(ACCOUNT3.getId()));
    }

    @Test
    public void testEnableAlreadyRegistered() {

        TwoFactorAuthDetails details2FA = service.enable(ACCOUNT1.getId());
        assertEquals(Status2FA.ALREADY_ENABLED, details2FA.getStatus2Fa());
    }
    @Test
    public void testEnableNotConfirmed() {
        TwoFactorAuthDetails authDetails = service.enable(ACCOUNT2.getId());
        TwoFactorAuthUtil.verifySecretCode(authDetails, Convert2.defaultRsAccount(ACCOUNT2.getId()));
        assertFalse(service.isEnabled(ACCOUNT2.getId()));
    }

    @Test
    public void testDisable() throws GeneralSecurityException {
        TwoFactorAuthService spy = spy(service);
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        spy.disable(ACCOUNT1.getId(), currentCode);
    }

    @Test
    public void testDisableFailAuth() {

        Status2FA status2FA = service.disable(ACCOUNT1.getId(), INVALID_CODE);

        assertEquals(Status2FA.INCORRECT_CODE, status2FA);
    }


    @Test
    public void testIsEnabledTrue() {

        boolean enabled = service.isEnabled(ACCOUNT1.getId());

        assertTrue(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenAccountIsNotExists() {

        boolean enabled = service.isEnabled(ACCOUNT3.getId());
        assertFalse(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenAccountIsNotConfirmed() {
        boolean enabled = service.isEnabled(ACCOUNT2.getId());
        assertFalse(enabled);
    }

    @Test
    public void testTryAuth() throws GeneralSecurityException {
        boolean authenticated = TwoFactorAuthUtil.tryAuth(service, ACCOUNT1.getId(), ACCOUNT1_2FA_SECRET_BASE32, MAX_2FA_ATTEMPTS);

        assertTrue(authenticated);
    }

    @Test
    public void testTryAuthCodesNotEquals() {
        int fakeNumber = new Random().nextInt();
        Status2FA status2FA = service.tryAuth(ACCOUNT1.getId(), fakeNumber);
        assertEquals(Status2FA.INCORRECT_CODE, status2FA);
    }

    @Test
    public void testTryAuthNotConfirmed() {
        int fakeNumber = new Random().nextInt();
        Status2FA status2FA = service.tryAuth(ACCOUNT2.getId(), fakeNumber);

        assertEquals(Status2FA.NOT_CONFIRMED, status2FA);
    }

    @Test
    public void testConfirm() throws GeneralSecurityException {
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT2_2FA_SECRET_BASE32);
        Status2FA status2FA = service.confirm(ACCOUNT2.getId(), currentCode);

        assertEquals(Status2FA.OK, status2FA);
        assertTrue(service.isEnabled(ACCOUNT2.getId()));
    }
    @Test
    public void testConfirmAlreadyConfirmed() throws GeneralSecurityException {
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
        Status2FA status2FA = service.confirm(ACCOUNT1.getId(), currentCode);
        assertEquals(Status2FA.ALREADY_CONFIRMED, status2FA);
        assertTrue(service.isEnabled(ACCOUNT1.getId()));
    }

    @Test
    public void testConfirmNotExists() throws GeneralSecurityException {
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT3_2FA_SECRET_BASE32);
        Status2FA status2FA = service.confirm(ACCOUNT3.getId(), currentCode);
        assertEquals(Status2FA.NOT_ENABLED,status2FA);
        assertFalse(service.isEnabled(ACCOUNT3.getId()));
    }
}
