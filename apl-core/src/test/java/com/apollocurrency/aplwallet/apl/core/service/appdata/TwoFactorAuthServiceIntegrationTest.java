/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata;

import com.apollocurrency.aplwallet.api.dto.auth.Status2FA;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.vault.model.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.vault.model.TwoFactorAuthEntity;
import com.apollocurrency.aplwallet.vault.service.auth.TwoFactorAuthFileSystemRepository;
import com.apollocurrency.aplwallet.vault.service.auth.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.vault.service.auth.TwoFactorAuthService;
import com.apollocurrency.aplwallet.vault.service.auth.TwoFactorAuthServiceImpl;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
/*import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;*/
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Random;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT3_2FA_SECRET_BASE32;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.INVALID_CODE;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.MAX_2FA_ATTEMPTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

@Slf4j

@Tag("slow")
@QuarkusTest
public class TwoFactorAuthServiceIntegrationTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer);
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
//    @WeldSetup
//    public WeldInitiator weld = WeldInitiator.from().addBeans(MockBean.of(Mockito.mock(NtpTime.class), NtpTime.class)).build();

    private TwoFactorAuthTestData td = new TwoFactorAuthTestData();
    private TwoFactorAuthRepository fileRepository;
    private TwoFactorAuthService service;

    @BeforeEach
    void setUp() throws IOException {
        fileRepository = new TwoFactorAuthFileSystemRepository(temporaryFolderExtension.getRoot().toPath());
        service = new TwoFactorAuthServiceImpl("test", fileRepository);

        File account1 = temporaryFolderExtension.newFile(Convert2.defaultRsAccount(td.ENTITY1.getAccount()));
        JSON.writeJson(account1, td.ENTITY1);
        File account2 = temporaryFolderExtension.newFile(Convert2.defaultRsAccount(td.ENTITY2.getAccount()));
        JSON.writeJson(account2, td.ENTITY2);
    }

    @BeforeAll
    static void beforeAll() {
        Convert2.init("APL", 1739068987193023818L);
    }


    @AfterEach
    void tearDown() {
        dbExtension.cleanAndPopulateDb();
    }

    @Test
    public void testEnable() {
        TwoFactorAuthDetails authDetails = service.enable(td.ACC_3.getId());

        assertTrue(authDetails.getQrCodeUrl().contains(authDetails.getSecret()));
        assertTrue(authDetails.getQrCodeUrl().startsWith(TimeBasedOneTimePasswordUtil.qrImageUrl(Convert2.defaultRsAccount(td.ACC_3.getId()),
            authDetails.getSecret())));
        assertFalse(service.isEnabled(td.ACC_3.getId()));
    }

    @Test
    public void testEnableAlreadyRegistered() {
        TwoFactorAuthDetails details2FA = service.enable(td.ACC_1.getId());

        assertEquals(Status2FA.ALREADY_ENABLED, details2FA.getStatus2Fa());
    }

    @Test
    public void testEnableNotConfirmed() {
        TwoFactorAuthDetails authDetails = service.enable(td.ACC_2.getId());

        assertTrue(authDetails.getQrCodeUrl().contains(authDetails.getSecret()));
        assertTrue(authDetails.getQrCodeUrl().startsWith(TimeBasedOneTimePasswordUtil.qrImageUrl(Convert2.defaultRsAccount(td.ACC_2.getId()),
            authDetails.getSecret())));
        assertFalse(service.isEnabled(td.ACC_2.getId()));
        // remove 2fa-file after test has passed
        boolean result = fileRepository.delete(td.ACC_2.getId());
        assertTrue(result);
    }

    @Test
    public void testDisable() throws GeneralSecurityException {
        TwoFactorAuthService spy = spy(service);
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT1_2FA_SECRET_BASE32);

        spy.disable(td.ACC_1.getId(), currentCode);
    }

    @Test
    public void testDisableFailAuth() {
        Status2FA status2FA = service.disable(td.ACC_1.getId(), INVALID_CODE);

        assertEquals(Status2FA.INCORRECT_CODE, status2FA);
    }


    @Test
    public void testIsEnabledTrue() {
        boolean enabled = service.isEnabled(td.ACC_1.getId());

        assertTrue(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenAccountIsNotExists() {
        boolean enabled = service.isEnabled(td.newAccount.getId());

        assertFalse(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenAccountIsNotConfirmed() {
        boolean enabled = service.isEnabled(td.ACC_2.getId());

        assertFalse(enabled);
    }

    @Test
    public void testTryAuth() throws GeneralSecurityException {
        boolean authenticated = tryAuth(service, td.ACC_1.getId(), td.ACCOUNT1_2FA_SECRET_BASE32, MAX_2FA_ATTEMPTS);

        assertTrue(authenticated);
    }

    @Test
    public void testTryAuthCodesNotEquals() {
        int fakeNumber = new Random().nextInt();
        fileRepository.add(new TwoFactorAuthEntity(td.ACC_1.getId(), td.ACCOUNT1_2FA_SECRET_BYTES, true));

        Status2FA status2FA = service.tryAuth(td.ACC_1.getId(), fakeNumber);
        assertEquals(Status2FA.INCORRECT_CODE, status2FA);
    }

    @Test
    public void testTryAuthNotConfirmed() {
        int fakeNumber = new Random().nextInt();
        Status2FA status2FA = service.tryAuth(td.ACC_2.getId(), fakeNumber);

        assertEquals(Status2FA.NOT_CONFIRMED, status2FA);
    }

    @Test
    public void testConfirm() throws GeneralSecurityException {
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT2_2FA_SECRET_BASE32);
        Status2FA status2FA = service.confirm(td.ACC_2.getId(), currentCode);

        assertEquals(Status2FA.OK, status2FA);
        assertTrue(service.isEnabled(td.ACC_2.getId()));
    }

    @Test
    public void testConfirmAlreadyConfirmed() throws GeneralSecurityException {
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT1_2FA_SECRET_BASE32);
        Status2FA status2FA = service.confirm(td.ACC_1.getId(), currentCode);

        assertEquals(Status2FA.ALREADY_CONFIRMED, status2FA);
        assertTrue(service.isEnabled(td.ACC_1.getId()));
    }

    @Test
    public void testConfirmNotExists() throws GeneralSecurityException {
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT3_2FA_SECRET_BASE32);
        Status2FA status2FA = service.confirm(td.newAccount.getId(), currentCode);

        assertEquals(Status2FA.NOT_ENABLED, status2FA);
        assertFalse(service.isEnabled(td.newAccount.getId()));
    }


    private boolean tryAuth(TwoFactorAuthService service, long account, String secret, int maxAttempts) throws GeneralSecurityException {
        // TimeBased code sometimes expire before calling tryAuth method, which will generate another code
        for (int i = 0; i < maxAttempts; i++) {
            int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(secret);
            Status2FA status2FA = service.tryAuth(account, currentNumber);
            if (status2FA == Status2FA.OK) {
                return true;
            }
        }
        return false;
    }

}
