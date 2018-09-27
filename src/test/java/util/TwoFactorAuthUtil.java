/*
 * Copyright © 2018 Apollo Foundation
 */

package util;

import com.apollocurrency.aplwallet.apl.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.TwoFactorAuthService;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import dto.Account;
import org.junit.Assert;

import java.security.GeneralSecurityException;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.*;

public class TwoFactorAuthUtil {
    private TwoFactorAuthUtil() {}

    public static boolean tryAuth(TwoFactorAuthService service) throws GeneralSecurityException {
        //        TimeBased code sometimes expire before calling tryAuth method, which will generate another code
        boolean authenticated = false;
        for (int i = 0; i < MAX_2FA_ATTEMPTS; i++) {
            long currentNumber = TimeBasedOneTimePasswordUtil.generateCurrentNumber(ACCOUNT1_2FA_SECRET_BASE32);
             authenticated = service.tryAuth(ACCOUNT1.getAccount(), currentNumber);
        }

        return authenticated;
    }

    public static void verifySecretCode(TwoFactorAuthDetails details, Account account) {
        Assert.assertTrue(details.getQrCodeUrl().contains(details.getSecret()));
        Assert.assertTrue(details.getQrCodeUrl().startsWith(TimeBasedOneTimePasswordUtil.qrImageUrl(account.getAccountRS(),
                details.getSecret())));
    }
}
