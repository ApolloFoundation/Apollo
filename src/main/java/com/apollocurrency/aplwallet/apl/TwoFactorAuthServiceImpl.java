/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthEntity;
import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.InvalidTwoFactorAuthCredentialsException;
import com.apollocurrency.aplwallet.apl.util.exception.TwoFactoAuthAlreadyRegisteredException;
import com.apollocurrency.aplwallet.apl.util.exception.UnknownTwoFactorAuthException;
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
//    transaction management required
    public TwoFactorAuthDetails enable(long accountId) {
//        check existing and not confirmed 2fa
        TwoFactorAuthEntity entity = repository.get(accountId);
        if (entity != null) {
            if (!entity.isConfirmed()) {

            String existingBase32Secret = BASE_32.encodeToString(entity.getSecret());
            return new TwoFactorAuthDetails(getQrCodeUrl(Convert.rsAccount(entity.getAccount()), existingBase32Secret),
                    existingBase32Secret);
            } else {
                throw new TwoFactoAuthAlreadyRegisteredException("Account has already enabled 2fa");
            }
        }
        //length of Base32Secret should be multiple 8 (length % 8 == 0); e.g. 8, 16, 24, 32, etc.
        //
        String base32Secret = TimeBasedOneTimePasswordUtil.generateBase32Secret(SECRET_LENGTH);
        byte[] base32Bytes = BASE_32.decode(base32Secret);
        boolean saved = repository.add(new TwoFactorAuthEntity(accountId, base32Bytes, false));
        if (!saved) {
            throw new UnknownTwoFactorAuthException("Unable to enable 2fa");
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
        TwoFactorAuthEntity entity = repository.get(accountId);
        if (authEntity(entity, authCode)) {
            //account with 2fa already exist
            repository.delete(accountId);
        } else {
            throw new InvalidTwoFactorAuthCredentialsException("2fa was failed");
        }
    }

    @Override
    public boolean isEnabled(long accountId) {
        TwoFactorAuthEntity entity = repository.get(accountId);
        return entity != null && entity.isConfirmed() && entity.getSecret() != null;
    }

    @Override
    public boolean tryAuth(long accountId, int authCode) {
        TwoFactorAuthEntity entity = repository.get(accountId);
        return authEntity(entity, authCode) && entity.isConfirmed();
    }

    private boolean authEntity(TwoFactorAuthEntity entity, int authCode) {

        boolean success = false;
        try {
            if (entity != null) {
                String base32Secret = BASE_32.encodeToString(entity.getSecret());
                //window millis should be 0, other parameters will not work properly
                success = TimeBasedOneTimePasswordUtil.validateCurrentNumber(
                        base32Secret, authCode, 0);
            }
        }
        catch (GeneralSecurityException e) {
            LOG.error("Unable to create temporal code", e);
        }
        return success;
    }

    @Override
    public boolean confirm(long accountId, int authCode) {
        TwoFactorAuthEntity entity = repository.get(accountId);
        if (entity != null && !entity.isConfirmed() && authEntity(entity, authCode)) {
            entity.setConfirmed(true);
            return repository.update(entity);
        } else return false;
    }
}
