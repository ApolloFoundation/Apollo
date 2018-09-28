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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;

import static org.slf4j.LoggerFactory.getLogger;

public class TwoFactorAuthServiceImpl implements TwoFactorAuthService {
    private static final Logger LOG = getLogger(TwoFactorAuthServiceImpl.class);
    private static final Base32 BASE_32 = new Base32();
    private static final String ISSUER_URL_PART = "&issuer=Apollo Wallet";
    private static final int SECRET_LENGTH = 32;

    private TwoFactorAuthRepository repository;


    public TwoFactorAuthServiceImpl(TwoFactorAuthRepository repository) {
        this.repository = repository;
    }

    @Override
    public TwoFactorAuthDetails enable(long accountId) {
        //length of Base32Secret should aliquot 8 (length % 8 == 0); e.g. 8, 16, 24, 32, etc.
        //
        String base32Secret = TimeBasedOneTimePasswordUtil.generateBase32Secret(SECRET_LENGTH);
        byte[] base32Bytes = BASE_32.decode(base32Secret);
        boolean saved = repository.saveSecret(accountId, base32Bytes);
        if (!saved) {
            throw new TwoFactoAuthAlreadyEnabledException("Account already has 2fa");
        }
        String qrCodeUrl = getQrCodeUrl(Convert.rsAccount(accountId), base32Secret);
        return new TwoFactorAuthDetails(qrCodeUrl, base32Secret);
    }

    private String getQrCodeUrl(String rsAccount, String base32Secret) {
        String baseUrl = TimeBasedOneTimePasswordUtil.qrImageUrl(rsAccount, base32Secret);
        try {
            return baseUrl + URLEncoder.encode(ISSUER_URL_PART, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public void disable(long accountId, int authCode) {
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
    public boolean tryAuth(long accountId, int authCode) {
        boolean succeed = false;
        try {
            byte[] secret = repository.getSecret(accountId);
            if (secret != null) {
                String base32Secret = BASE_32.encodeToString(secret);
                //window millis should be 0, other parameters will not work properly
                succeed = TimeBasedOneTimePasswordUtil.validateCurrentNumber(
                        base32Secret, authCode, 0);
            }
        }
        catch (GeneralSecurityException e) {
            LOG.error("Unable to create temporal code", e);
        }
        return succeed;
    }
}
