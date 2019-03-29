/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

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
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import com.apollocurrency.aplwallet.apl.testutil.TwoFactorAuthUtil;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import java.security.GeneralSecurityException;
import java.util.Random;

@EnableWeld
public class TwoFactorAuthServiceIntegrationTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from().addBeans(MockBean.of(Mockito.mock(NtpTime.class), NtpTime.class)).build();

    private TwoFactorAuthRepository repository = new TwoFactorAuthRepositoryImpl(dbExtension.getDatabaseManger().getDataSource());
    private TwoFactorAuthService    service = new TwoFactorAuthServiceImpl(repository, "test");

    @Test
    public void testEnable() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        TwoFactorAuthDetails authDetails = service.enable(td.ACCOUNT3.getId());
        TwoFactorAuthUtil.verifySecretCode(authDetails, Convert2.defaultRsAccount(td.ACCOUNT3.getId()));
        assertFalse(service.isEnabled(td.ACCOUNT3.getId()));
    }

    @Test
    public void testEnableAlreadyRegistered() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        TwoFactorAuthDetails details2FA = service.enable(td.ACCOUNT1.getId());
        assertEquals(Status2FA.ALREADY_ENABLED, details2FA.getStatus2Fa());
    }
    @Test
    public void testEnableNotConfirmed() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        TwoFactorAuthDetails authDetails = service.enable(td.ACCOUNT2.getId());
        TwoFactorAuthUtil.verifySecretCode(authDetails, Convert2.defaultRsAccount(td.ACCOUNT2.getId()));
        assertFalse(service.isEnabled(td.ACCOUNT2.getId()));
    }

    @Test
    public void testDisable() throws GeneralSecurityException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        TwoFactorAuthService spy = spy(service);
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT1_2FA_SECRET_BASE32);
        spy.disable(td.ACCOUNT1.getId(), currentCode);
    }

    @Test
    public void testDisableFailAuth() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        Status2FA status2FA = service.disable(td.ACCOUNT1.getId(), INVALID_CODE);

        assertEquals(Status2FA.INCORRECT_CODE, status2FA);
    }


    @Test
    public void testIsEnabledTrue() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        boolean enabled = service.isEnabled(td.ACCOUNT1.getId());

        assertTrue(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenAccountIsNotExists() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        boolean enabled = service.isEnabled(td.ACCOUNT3.getId());
        assertFalse(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenAccountIsNotConfirmed() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        boolean enabled = service.isEnabled(td.ACCOUNT2.getId());
        assertFalse(enabled);
    }

    @Test
    public void testTryAuth() throws GeneralSecurityException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        boolean authenticated = TwoFactorAuthUtil.tryAuth(service, td.ACCOUNT1.getId(), td.ACCOUNT1_2FA_SECRET_BASE32, MAX_2FA_ATTEMPTS);

        assertTrue(authenticated);
    }

    @Test
    public void testTryAuthCodesNotEquals() {
        int fakeNumber = new Random().nextInt();
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        Status2FA status2FA = service.tryAuth(td.ACCOUNT1.getId(), fakeNumber);
        assertEquals(Status2FA.INCORRECT_CODE, status2FA);
    }

    @Test
    public void testTryAuthNotConfirmed() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        int fakeNumber = new Random().nextInt();
        Status2FA status2FA = service.tryAuth(td.ACCOUNT2.getId(), fakeNumber);

        assertEquals(Status2FA.NOT_CONFIRMED, status2FA);
    }

    @Test
    public void testConfirm() throws GeneralSecurityException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT2_2FA_SECRET_BASE32);
        Status2FA status2FA = service.confirm(td.ACCOUNT2.getId(), currentCode);

        assertEquals(Status2FA.OK, status2FA);
        assertTrue(service.isEnabled(td.ACCOUNT2.getId()));
    }
    @Test
    public void testConfirmAlreadyConfirmed() throws GeneralSecurityException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT1_2FA_SECRET_BASE32);
        Status2FA status2FA = service.confirm(td.ACCOUNT1.getId(), currentCode);
        assertEquals(Status2FA.ALREADY_CONFIRMED, status2FA);
        assertTrue(service.isEnabled(td.ACCOUNT1.getId()));
    }

    @Test
    public void testConfirmNotExists() throws GeneralSecurityException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT3_2FA_SECRET_BASE32);
        Status2FA status2FA = service.confirm(td.ACCOUNT3.getId(), currentCode);
        assertEquals(Status2FA.NOT_ENABLED,status2FA);
        assertFalse(service.isEnabled(td.ACCOUNT3.getId()));
    }
}
