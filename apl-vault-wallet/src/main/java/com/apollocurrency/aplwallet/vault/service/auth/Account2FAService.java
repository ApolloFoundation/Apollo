/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.vault.service.auth;

import com.apollocurrency.aplwallet.api.dto.auth.Status2FA;
import com.apollocurrency.aplwallet.api.dto.auth.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.util.service.ElGamalEncryptor;
import com.apollocurrency.aplwallet.apl.util.service.PassphraseGeneratorImpl;
import com.apollocurrency.aplwallet.vault.KeyStoreService;
import com.apollocurrency.aplwallet.vault.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.vault.model.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.vault.util.AccountHelper;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This class is just static helper for 2FA.
 *
 * @author al
 * @author az
 */
@Slf4j
@Singleton
public class Account2FAService {
    public static final String TWO_FACTOR_AUTH_PARAMETERS_ATTRIBUTE_NAME = "twoFactorAuthParameters";
    public static final String PASSPHRASE_PARAM_NAME = "passphrase";
    public static final String SECRET_PHRASE_PARAM_NAME = "secretPhrase";
    public static final String CODE2FA_PARAM_NAME = "code2FA";
    public static final String PUBLIC_KEY_PARAM_NAME = "publicKey";

    private final TwoFactorAuthService service2FA;
    private final ElGamalEncryptor elGamal;
    private final KeyStoreService keyStoreService;
    private final PassphraseGeneratorImpl passphraseGenerator = new PassphraseGeneratorImpl();

    @Inject
    public Account2FAService(TwoFactorAuthService service2FA, KeyStoreService keyStoreService, ElGamalEncryptor elGamal) {
        this.keyStoreService = keyStoreService;
        this.elGamal = elGamal;
        this.service2FA = service2FA;
    }

    /**
     * Analise incoming parameters and create TwoFactorAuthParameters instance. Throw the exception if parameters are wrong or inconsistent.
     *
     * @param accountStr        account id
     * @param passphraseParam   pass phrase
     * @param secretPhraseParam secret phrase
     * @param publicKeyParam    public key
     * @return new instance of TwoFactorAuthParameters
     * @throws RestParameterException
     */
    public TwoFactorAuthParameters create2FAParameters(String accountStr, String passphraseParam, String secretPhraseParam, String publicKeyParam) throws RestParameterException {
        if (StringUtils.isBlank(passphraseParam) && StringUtils.isBlank(secretPhraseParam)) {
            throw new RestParameterException(ApiErrors.MISSING_PARAM_LIST, "passphrase, secretPhrase");
        }

        if (StringUtils.isNotBlank(passphraseParam) && StringUtils.isNotBlank(secretPhraseParam)) {
            throw new RestParameterException(ApiErrors.ONLY_ONE_OF_PARAM_LIST, "passphrase, secretPhrase");
        }

        String passphrase = null;
        if (StringUtils.isNotBlank(passphraseParam)) {
            if (StringUtils.isBlank(accountStr)) {
                throw new RestParameterException(ApiErrors.MISSING_PARAM, "account");
            }
            passphrase = elGamal.elGamalDecrypt(passphraseParam);
        }

        String secretPhrase = null;
        if (StringUtils.isNotBlank(secretPhraseParam)) {
            secretPhrase = elGamal.elGamalDecrypt(secretPhraseParam);
        }

        long accountId = 0;
        byte[] publicKey = null;
        if (passphrase != null) {
            accountId = Convert.parseAccountId(accountStr);
        } else {
            if (secretPhrase != null) {
                publicKey = Crypto.getPublicKey(secretPhrase);
                accountId = Convert.getId(publicKey);
            } else if (publicKeyParam != null) {
                publicKey = Convert.parseHexString(Convert.emptyToNull(publicKeyParam));
                if (Crypto.isCanonicalPublicKey(publicKey)) {
                    accountId = Convert.getId(publicKey);
                }
            }
        }
        if (accountId == 0) {
            throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, "account id or passphrase");
        }

        return new TwoFactorAuthParameters(accountId, passphrase, secretPhrase, publicKey);
    }

    public TwoFactorAuthDetails enable2FA(TwoFactorAuthParameters params2FA) {
        TwoFactorAuthDetails authDetails;
        if (params2FA.isPassphrasePresent()) {
            findAplSecretBytes(params2FA.getAccountId(), params2FA.getPassphrase());
        }
        authDetails = service2FA.enable(params2FA.getAccountId());
        return authDetails;
    }

    public Status2FA disable2FA(TwoFactorAuthParameters params2FA) {
        Status2FA status2FA = disable2FA(params2FA.getAccountId(), params2FA.getPassphrase(), params2FA.getCode2FA());
        params2FA.setStatus2FA(status2FA);
        return status2FA;
    }

    public Status2FA disable2FA(long accountId, String passphrase, Integer code) {
        if (passphrase != null) {
            findAplSecretBytes(accountId, passphrase);
        }
        Status2FA status2FA = service2FA.disable(accountId, code);
        validate2FAStatus(status2FA, accountId);
        return status2FA;
    }

    public Status2FA confirm2FA(TwoFactorAuthParameters params2FA) {
        Status2FA status2FA;
        if (params2FA.getCode2FA() == null) {
            throw new RestParameterException(ApiErrors.MISSING_PARAM, "code2FA");
        }
        if (params2FA.isPassphrasePresent()) {
            findAplSecretBytes(params2FA.getAccountId(), params2FA.getPassphrase());
        }
        status2FA = service2FA.confirm(params2FA.getAccountId(), params2FA.getCode2FA());
        validate2FAStatus(status2FA, params2FA.getAccountId());
        return status2FA;
    }

    public TwoFactorAuthParameters verify2FA(String accountStr,
                                             String passphraseParam,
                                             String secretPhraseParam,
                                             String publicKeyParam,
                                             Integer code2FA) throws RestParameterException {
        TwoFactorAuthParameters params2FA = create2FAParameters(accountStr,
            passphraseParam, secretPhraseParam, publicKeyParam);
        params2FA.setCode2FA(code2FA);
        params2FA.setStatus2FA(
            verify2FA(params2FA)
        );
        return params2FA;
    }

    public Status2FA verify2FA(TwoFactorAuthParameters params2FA) throws RestParameterException {
        Status2FA status2FA = Status2FA.NOT_ENABLED;
        if (isEnabled2FA(params2FA.getAccountId())) {
            if (params2FA.getCode2FA() == null) {
                throw new RestParameterException(ApiErrors.MISSING_PARAM, "code2FA");
            }

            if (params2FA.isPassphrasePresent()) {
                findAplSecretBytes(params2FA.getAccountId(), params2FA.getPassphrase());
                status2FA = service2FA.tryAuth(params2FA.getAccountId(), params2FA.getCode2FA());
                validate2FAStatus(status2FA, params2FA.getAccountId());
            } else {
                long accountId = Convert.getId(Crypto.getPublicKey(params2FA.getSecretPhrase()));
                status2FA = service2FA.tryAuth(accountId, params2FA.getCode2FA());
                validate2FAStatus(status2FA, accountId);
            }
        }
        return status2FA;
    }

    public boolean isEnabled2FA(long accountId) {
        return service2FA.isEnabled(accountId);
    }

    public KeyStoreService.Status deleteAccount(long accountId, String passphrase, Integer code) throws RestParameterException {
        if (isEnabled2FA(accountId)) {
            if (code == null) {
                throw new RestParameterException(ApiErrors.MISSING_PARAM, "code2FA");
            }
            Status2FA status2FA = disable2FA(accountId, passphrase, code);
            validate2FAStatus(status2FA, accountId);
        }
        KeyStoreService.Status status = keyStoreService.deleteKeyStore(passphrase, accountId);
        validateKeyStoreStatus(accountId, status, "deleted");
        return status;
    }

    public KeyStoreService.Status deleteAccount(TwoFactorAuthParameters twoFactorAuthParameters) throws RestParameterException {
        return deleteAccount(twoFactorAuthParameters.getAccountId(), twoFactorAuthParameters.getPassphrase(), twoFactorAuthParameters.getCode2FA());
    }

    public byte[] findAplSecretBytes(long accountId, String passphrase) throws RestParameterException {
        ApolloFbWallet fbWallet = keyStoreService.getSecretStore(passphrase, accountId);
        if (fbWallet == null) {
            throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, String.format("%s, account=%d", "account id or passphrase", accountId));
        }
        return Convert.parseHexString(fbWallet.getAplKeySecret());
    }

    public byte[] findAplSecretBytes(TwoFactorAuthParameters twoFactorAuthParameters) throws RestParameterException {
        return findAplSecretBytes(twoFactorAuthParameters.getAccountId(), twoFactorAuthParameters.getPassphrase());
    }

    public WalletKeysInfo generateUserWallet(String passphrase) throws RestParameterException {
        return generateUserWallet(passphrase, null);
    }

    public WalletKeysInfo generateUserWallet(String passphrase, byte[] secretApl) throws RestParameterException {
        if (passphrase == null) {
            passphrase = passphraseGenerator.generate();
        }

        ApolloFbWallet apolloWallet = AccountHelper.generateApolloWallet(secretApl);

        long aplId = apolloWallet.getAplWalletKey().getId();

        KeyStoreService.Status status = keyStoreService.saveSecretKeyStore(passphrase, aplId, apolloWallet);
        validateKeyStoreStatus(aplId, status, "generated");

        WalletKeysInfo walletKeyInfo = new WalletKeysInfo(apolloWallet, passphrase);

        return walletKeyInfo;
    }


    private void validate2FAStatus(Status2FA status2FA, long accountId) throws RestParameterException {
        if (status2FA != Status2FA.OK) {
            log.debug("2fa error: {}-{}", Convert2.rsAccount(accountId), status2FA);
            throw new RestParameterException(ApiErrors.ACCOUNT_2FA_ERROR, String.format("%s, account=%d", status2FA.name(), accountId));
        }
    }

    private void validateKeyStoreStatus(long accountId, KeyStoreService.Status status, String notPerformedAction) {
        if (status != KeyStoreService.Status.OK) {
            log.debug("Vault wallet not " + notPerformedAction + " {} - {}", Convert2.rsAccount(accountId), status);
            throw new RestParameterException(ApiErrors.ACCOUNT_2FA_ERROR, String.format("Vault wallet for account was not %s : %s", notPerformedAction, status.message));
        }
    }

}
