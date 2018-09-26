/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.InvalidTwoFactorAuthCredentialsException;
import com.apollocurrency.aplwallet.apl.util.exception.TwoFactoAuthAlreadyEnabledException;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;

import java.security.GeneralSecurityException;

import static com.j256.twofactorauth.TimeBasedOneTimePasswordUtil.DEFAULT_TIME_STEP_SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

public class TwoFactorAuthServiceImpl implements TwoFactorAuthService {
    private static final Logger LOG = getLogger(TwoFactorAuthServiceImpl.class);
    private static final Base32 BASE_32 = new Base32();

    private TwoFactorAuthRepository repository;


    public TwoFactorAuthServiceImpl(TwoFactorAuthRepository repository) {
        this.repository = repository;
    }

    @Override
    public TwoFactorAuthDetails enable(long accountId) {
        String base32Secret = TimeBasedOneTimePasswordUtil.generateBase32Secret(50);
        byte[] base32Bytes = BASE_32.decode(base32Secret);
        boolean saved = repository.saveSecret(accountId, base32Bytes);
        if (!saved) {
            throw new TwoFactoAuthAlreadyEnabledException("Account already has 2fa");
        }
        String qrCodeUrl = TimeBasedOneTimePasswordUtil.qrImageUrl(Convert.rsAccount(accountId), base32Secret);
        return new TwoFactorAuthDetails(qrCodeUrl, base32Secret);
    }

    @Override
    public void disable(long accountId, long authCode) {
        if (tryAuth(accountId, authCode)) {
            //account with 2fa already exist
            repository.delete(accountId);
        } else {
            throw new InvalidTwoFactorAuthCredentialsException("2fa was failed");
        }
    }

    @Override
    public boolean isEnabled(long accountId) {
        byte[] secret = repository.getSecret(accountId);
        return secret != null && secret.length != 0;
    }

    @Override
    public boolean tryAuth(long accountId, long authCode) {
        boolean succeed = false;
        try {
            byte[] secret = repository.getSecret(accountId);
            if (secret != null) {
                long temporalCode = TimeBasedOneTimePasswordUtil.generateNumber(BASE_32.encodeToString(secret), System.currentTimeMillis(),
                        DEFAULT_TIME_STEP_SECONDS);
                succeed = temporalCode == authCode;
            }
        }
        catch (GeneralSecurityException e) {
            LOG.error("Unable to create temporal code", e);
        }
        return succeed;
    }
}
