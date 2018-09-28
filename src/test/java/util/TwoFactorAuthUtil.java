/*
 * Copyright © 2018 Apollo Foundation
 */

package util;

import com.apollocurrency.aplwallet.apl.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.TwoFactorAuthService;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import org.junit.Assert;

import java.security.GeneralSecurityException;

public class TwoFactorAuthUtil {
    private TwoFactorAuthUtil() {}

    public static boolean tryAuth(TwoFactorAuthService service, long account, String secret, int maxAttempts) throws GeneralSecurityException {
        // TimeBased code sometimes expire before calling tryAuth method, which will generate another code
        boolean authenticated = false;
        for (int i = 0; i < maxAttempts; i++) {
            int currentNumber = (int) TimeBasedOneTimePasswordUtil.generateCurrentNumber(secret);
             authenticated = service.tryAuth(account, currentNumber);
        }
        return authenticated;
    }

    public static void verifySecretCode(TwoFactorAuthDetails details, String accountRS) {
        Assert.assertTrue(details.getQrCodeUrl().contains(details.getSecret()));
        Assert.assertTrue(details.getQrCodeUrl().startsWith(TimeBasedOneTimePasswordUtil.qrImageUrl(accountRS,
                details.getSecret())));
    }
}
