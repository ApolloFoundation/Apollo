/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthService;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;

import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TwoFactorAuthUtil {
    private TwoFactorAuthUtil() {
    }

    public static boolean tryAuth(TwoFactorAuthService service, long account, String secret, int maxAttempts) throws GeneralSecurityException {
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

    public static void verifySecretCode(TwoFactorAuthDetails details, String accountRS) {
        assertTrue(details.getQrCodeUrl().contains(details.getSecret()));
        assertTrue(details.getQrCodeUrl().startsWith(TimeBasedOneTimePasswordUtil.qrImageUrl(accountRS,
            details.getSecret())));
    }
}
