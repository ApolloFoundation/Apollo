/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.INVALID_CODE;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.MAX_2FA_ATTEMPTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.security.GeneralSecurityException;
import java.util.Random;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthService;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthServiceImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import com.apollocurrency.aplwallet.apl.testutil.TwoFactorAuthUtil;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TwoFactorAuthServiceTest {
    @Mock
    private TwoFactorAuthRepository repository;
    private TwoFactorAuthService service;

    @BeforeEach
    public void setUp() throws Exception {
        service = new TwoFactorAuthServiceImpl(repository, "test");
    }

    @Test
    public void testEnable() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();                
        doReturn(true).when(repository).add(any(TwoFactorAuthEntity.class));
        TwoFactorAuthDetails twoFactorAuthDetails = service.enable(td.ENTITY3.getAccount());
        TwoFactorAuthUtil.verifySecretCode(twoFactorAuthDetails, Convert.defaultRsAccount(td.ENTITY3.getAccount()));
        assertEquals(Status2FA.OK, twoFactorAuthDetails.getStatus2Fa());
        verify(repository, times(1)).add(any(TwoFactorAuthEntity.class));
    }

    @Test
    public void testEnableAlreadyEnabled() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();        
        doReturn(td.ENTITY1).when(repository).get(td.ACCOUNT1.getId());

        TwoFactorAuthDetails details = service.enable(td.ACCOUNT1.getId());

        assertEquals(Status2FA.ALREADY_ENABLED, details.getStatus2Fa());
    }
    @Test
    public void testEnableNotConfirmed() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();        
       
        doReturn(td.ENTITY2).when(repository).get(td.ACCOUNT2.getId());

        TwoFactorAuthDetails details = service.enable(td.ACCOUNT2.getId());
        TwoFactorAuthUtil.verifySecretCode(details, Convert.defaultRsAccount(td.ACCOUNT2.getId()));
        assertEquals(td.ACCOUNT2_2FA_SECRET_BASE32, details.getSecret());
    }


    @Test
    public void testDisable() throws GeneralSecurityException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();                
        TwoFactorAuthService spy = spy(service);
        int code = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT1_2FA_SECRET_BASE32);
        doReturn(td.ENTITY1).when(repository).get(td.ACCOUNT1.getId());

        spy.disable(td.ACCOUNT1.getId(), code);

        verify(repository, times(1)).get(td.ACCOUNT1.getId());
        verify(repository, times(1)).delete(td.ACCOUNT1.getId());
    }

    @Test
    public void testDisableFailAuth() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();                
        TwoFactorAuthService spy = spy(service);
        doReturn(td.ENTITY1).when(repository).get(td.ACCOUNT1.getId());

        Status2FA status2FA = spy.disable(td.ACCOUNT1.getId(), INVALID_CODE);
        assertEquals(Status2FA.INCORRECT_CODE, status2FA);
    }


    @Test
    public void testIsEnabledTrue() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();        
       
        doReturn(td.ENTITY1).when(repository).get(td.ACCOUNT1.getId());

        boolean enabled = service.isEnabled(td.ACCOUNT1.getId());

        verify(repository, times(1)).get(td.ACCOUNT1.getId());

        assertTrue(enabled);
    }

    @Test
    public void testIsEnabledFalse() {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();        

        boolean enabled = service.isEnabled(td.ACCOUNT1.getId());

        verify(repository, times(1)).get(td.ACCOUNT1.getId());

        assertFalse(enabled);
    }
    @Test
    public void testIsEnabledFalseWhen2faWasNotConfirmed() throws CloneNotSupportedException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();        
        
        doReturn(td.ENTITY2.clone()).when(repository).get(td.ACCOUNT2.getId());

        boolean enabled = service.isEnabled(td.ACCOUNT2.getId());

        verify(repository, times(1)).get(td.ACCOUNT2.getId());

        assertFalse(enabled);
    }


    @Test
    public void testTryAuth() throws GeneralSecurityException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();                
        doReturn(td.ENTITY1).when(repository).get(td.ACCOUNT1.getId());

        boolean authenticated = TwoFactorAuthUtil.tryAuth(service, td.ACCOUNT1.getId(), td.ACCOUNT1_2FA_SECRET_BASE32, MAX_2FA_ATTEMPTS);
        verify(repository, atMost(MAX_2FA_ATTEMPTS)).get(td.ACCOUNT1.getId());

        assertTrue(authenticated);
    }

    @Test
    public void testTryAuthCodesNotEquals() throws GeneralSecurityException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();        
        
        doReturn(td.ENTITY1).when(repository).get(td.ACCOUNT1.getId());

        int currentNumber = new Random().nextInt();
        Status2FA status2FA = service.tryAuth(td.ACCOUNT1.getId(), currentNumber);

        verify(repository, times(1)).get(td.ACCOUNT1.getId());

        assertEquals(Status2FA.INCORRECT_CODE, status2FA);
    }

    @Test
    public void testTryAuthNotFoundSecretForAccount() throws GeneralSecurityException {
//        just to make sure that 2fa code is correct
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();        

        int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT1_2FA_SECRET_BASE32);
        Status2FA status2FA = service.tryAuth(td.ACCOUNT1.getId(), currentNumber);

        verify(repository, times(1)).get(td.ACCOUNT1.getId());

        assertEquals(Status2FA.NOT_ENABLED, status2FA);
    }
    @Test
    public void testTryAuthForNotConfirmedAccount() throws GeneralSecurityException, CloneNotSupportedException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();                
        doReturn(td.ENTITY2.clone()).when(repository).get(td.ACCOUNT2.getId());
        int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT2_2FA_SECRET_BASE32);
        Status2FA status2FA = service.tryAuth(td.ACCOUNT2.getId(), currentNumber);

        verify(repository, times(1)).get(td.ACCOUNT2.getId());

        assertEquals(Status2FA.NOT_CONFIRMED, status2FA);
    }
    @Test
    public void testConfirm() throws GeneralSecurityException, CloneNotSupportedException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();        
        
        TwoFactorAuthEntity clone = td.ENTITY2.clone();
        doReturn(clone).when(repository).get(td.ACCOUNT2.getId());
        doReturn(true).when(repository).update(clone);

        int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT2_2FA_SECRET_BASE32);
        Status2FA status2FA = service.confirm(td.ACCOUNT2.getId(), currentNumber);

        verify(repository, times(1)).get(td.ACCOUNT2.getId());
        verify(repository, times(1)).update(clone);

        assertEquals(Status2FA.OK, status2FA);
    }

    @Test
    public void testConfirmForAlreadyEnabledAccount() throws GeneralSecurityException, CloneNotSupportedException {
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();        
        
        doReturn(td.ENTITY1.clone()).when(repository).get(td.ACCOUNT1.getId());
        int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(td.ACCOUNT1_2FA_SECRET_BASE32);
        Status2FA status2FA = service.confirm(td.ACCOUNT1.getId(), currentNumber);

        verify(repository, times(1)).get(td.ACCOUNT1.getId());

        assertEquals(Status2FA.ALREADY_CONFIRMED, status2FA);
    }
}
