/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.TwoFactorAuthFileSystemRepository;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.TwoFactorAuthRepositoryImpl;
import com.apollocurrency.aplwallet.apl.core.model.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TwoFactorAuthServiceImpl;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.TwoFactorAuthUtil;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import java.security.GeneralSecurityException;
import java.util.Random;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT3_2FA_SECRET_BASE32;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.INVALID_CODE;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.MAX_2FA_ATTEMPTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@Slf4j

@Tag("slow")
@EnableWeld
public class TwoFactorAuthServiceIntegrationTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer);
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from().addBeans(MockBean.of(Mockito.mock(NtpTime.class), NtpTime.class)).build();

    private TwoFactorAuthRepository dbRepository = new TwoFactorAuthRepositoryImpl(dbExtension.getDatabaseManager().getDataSource());
    private TwoFactorAuthRepository fileRepository = new TwoFactorAuthFileSystemRepository(temporaryFolderExtension.getRoot().toPath());
    private TwoFactorAuthService service;// = new TwoFactorAuthServiceImpl(repository, "test", targetFileRepository);

    @AfterEach
    void tearDown() {
        dbExtension.cleanAndPopulateDb();
    }

    @Test
    public void testEnable() {
        initConfigPrefix();
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        service = new TwoFactorAuthServiceImpl(dbRepository, "test", fileRepository);
        TwoFactorAuthDetails authDetails = service.enable(td.ACC_3.getId());
        TwoFactorAuthUtil.verifySecretCode(authDetails, Convert2.defaultRsAccount(td.ACC_3.getId()));
        assertFalse(service.isEnabled(td.ACC_3.getId()));
    }


    private void initConfigPrefix() {
        BlockchainConfig config = mock(BlockchainConfig.class);
        doReturn("APL").when(config).getAccountPrefix();
        Convert2.init(config);
    }

    @Test
    public void testEnableAlreadyRegistered() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        service = new TwoFactorAuthServiceImpl(fileRepository, "test", dbRepository); // switch repos for test only
        TwoFactorAuthDetails details2FA = service.enable(td.ACC_1.getId());
        assertEquals(Status2FA.ALREADY_ENABLED, details2FA.getStatus2Fa());
    }

    @Test
    public void testEnableNotConfirmed() {
        initConfigPrefix();
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        service = new TwoFactorAuthServiceImpl(dbRepository, "test", fileRepository);
        TwoFactorAuthDetails authDetails = service.enable(td.ACC_2.getId());
        TwoFactorAuthUtil.verifySecretCode(authDetails, Convert2.defaultRsAccount(td.ACC_2.getId()));
        assertFalse(service.isEnabled(td.ACC_2.getId()));
        // remove 2fa-file after test has passed
        boolean result = fileRepository.delete(td.ACC_2.getId());
        assertTrue(result);
    }

    @Test
    public void testDisable() throws GeneralSecurityException {
        initConfigPrefix();
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        service = new TwoFactorAuthServiceImpl(dbRepository, "test", fileRepository);
        TwoFactorAuthService spy = spy(service);
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT1_2FA_SECRET_BASE32);
        spy.disable(td.ACC_1.getId(), currentCode);
    }

    @Test
    public void testDisableFailAuth() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        service = new TwoFactorAuthServiceImpl(fileRepository, "test", dbRepository); // switch repos for test only
        Status2FA status2FA = service.disable(td.ACC_1.getId(), INVALID_CODE);
        assertEquals(Status2FA.INCORRECT_CODE, status2FA);
    }


    @Test
    public void testIsEnabledTrue() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        service = new TwoFactorAuthServiceImpl(fileRepository, "test", dbRepository); // switch repos for test only
        boolean enabled = service.isEnabled(td.ACC_1.getId());
        assertTrue(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenAccountIsNotExists() {
        initConfigPrefix();
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        service = new TwoFactorAuthServiceImpl(dbRepository, "test", fileRepository);
        boolean enabled = service.isEnabled(td.newAccount.getId());
        assertFalse(enabled);
    }

    @Test
    public void testIsEnabledFalseWhenAccountIsNotConfirmed() {
        initConfigPrefix();
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        service = new TwoFactorAuthServiceImpl(dbRepository, "test", fileRepository);
        boolean enabled = service.isEnabled(td.ACC_2.getId());
        assertFalse(enabled);
    }

    @Test
    public void testTryAuth() throws GeneralSecurityException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        service = new TwoFactorAuthServiceImpl(fileRepository, "test", dbRepository); // switch repos for test only
        boolean authenticated = TwoFactorAuthUtil.tryAuth(service, td.ACC_1.getId(), td.ACCOUNT1_2FA_SECRET_BASE32, MAX_2FA_ATTEMPTS);
        assertTrue(authenticated);
    }

    @Test
    public void testTryAuthCodesNotEquals() {
        initConfigPrefix();
        int fakeNumber = new Random().nextInt();
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        service = new TwoFactorAuthServiceImpl(dbRepository, "test", fileRepository);
        Status2FA status2FA = service.tryAuth(td.ACC_1.getId(), fakeNumber);
        assertEquals(Status2FA.INCORRECT_CODE, status2FA);
    }

    @Test
    public void testTryAuthNotConfirmed() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        int fakeNumber = new Random().nextInt();
        service = new TwoFactorAuthServiceImpl(fileRepository, "test", dbRepository); // switch repos for test only
        Status2FA status2FA = service.tryAuth(td.ACC_2.getId(), fakeNumber);
        assertEquals(Status2FA.NOT_CONFIRMED, status2FA);
    }

    @Test
    public void testConfirm() throws GeneralSecurityException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT2_2FA_SECRET_BASE32);
        service = new TwoFactorAuthServiceImpl(fileRepository, "test", dbRepository); // switch repos for test only
        Status2FA status2FA = service.confirm(td.ACC_2.getId(), currentCode);
        assertEquals(Status2FA.OK, status2FA);
        assertTrue(service.isEnabled(td.ACC_2.getId()));
    }

    @Test
    public void testConfirmAlreadyConfirmed() throws GeneralSecurityException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT1_2FA_SECRET_BASE32);
        service = new TwoFactorAuthServiceImpl(fileRepository, "test", dbRepository); // switch repos for test only
        Status2FA status2FA = service.confirm(td.ACC_1.getId(), currentCode);
        assertEquals(Status2FA.ALREADY_CONFIRMED, status2FA);
        assertTrue(service.isEnabled(td.ACC_1.getId()));
    }

    @Test
    public void testConfirmNotExists() throws GeneralSecurityException {
        initConfigPrefix();
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        int currentCode = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT3_2FA_SECRET_BASE32);
        service = new TwoFactorAuthServiceImpl(dbRepository, "test", fileRepository);
        Status2FA status2FA = service.confirm(td.newAccount.getId(), currentCode);
        assertEquals(Status2FA.NOT_ENABLED, status2FA);
        assertFalse(service.isEnabled(td.newAccount.getId()));
    }

    @Test
    public void testMoveData() throws GeneralSecurityException {
        initConfigPrefix();
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        service = new TwoFactorAuthServiceImpl(dbRepository, "test", fileRepository);
        int result = service.attemptMoveDataFromDatabase();
        assertEquals(2, result, "Error on moving data from DB into File repo");
        assertFalse(service.isEnabled(td.ACC_3.getId()));
    }
}
