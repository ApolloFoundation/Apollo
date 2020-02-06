/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.utils;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.app.PassphraseGeneratorImpl;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthService;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthServiceImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthFileSystemRepository;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthRepositoryImpl;
import com.apollocurrency.aplwallet.apl.core.http.ElGamalEncryptor;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountBalanceService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
/**
 * This class is just static helper for 2FA.
 * @author al
 * @author az
 */
@Slf4j
@Singleton
public class Account2FAHelper {

    private final TwoFactorAuthService service2FA;
    private final PassphraseGeneratorImpl passphraseGenerator;


    private final ElGamalEncryptor elGamal;
    private final KeyStoreService keyStoreService;
    private final AccountBalanceService accountService;

    @Inject
    public Account2FAHelper(DatabaseManager databaseManager, PropertiesHolder propertiesHolder, DirProvider dirProvider, KeyStoreService keyStoreService, ElGamalEncryptor elGamal, AccountBalanceService accountService) {
        this.keyStoreService = keyStoreService;
        this.elGamal = elGamal;
        this.accountService = accountService;
        this.passphraseGenerator = new PassphraseGeneratorImpl(10, 15);
        service2FA = new TwoFactorAuthServiceImpl(
                propertiesHolder.getBooleanProperty("apl.store2FAInFileSystem")
                        ? new TwoFactorAuthFileSystemRepository(dirProvider.get2FADir())
                        : new TwoFactorAuthRepositoryImpl(databaseManager.getDataSource()),
                propertiesHolder.getStringProperty("apl.issuerSuffix2FA", RuntimeEnvironment.getInstance().isDesktopApplicationEnabled() ? "desktop" : "web"));
    }

    public TwoFactorAuthParameters validate2FAParameters(TwoFactorAuthParameters parameters){
        String accountStr = Long.toUnsignedString(parameters.getAccountId());
        String passphrase = parameters.getPassphrase();
        String secretPhrase = parameters.getSecretPhrase();
        Integer code2FA = parameters.getCode2FA();
        if (code2FA == null){
            throw new RestParameterException(ApiErrors.MISSING_PARAM, "code2FA");
        }

        return parse2FARequestParams(accountStr, passphrase, secretPhrase);
    }

    public TwoFactorAuthParameters parse2FARequestParams(String accountStr, String passphraseParam, String secretPhraseParam) throws RestParameterException{
        if (StringUtils.isBlank(passphraseParam) && StringUtils.isBlank(secretPhraseParam)){
            throw new RestParameterException(ApiErrors.MISSING_PARAM_LIST, "passphrase, secretPhrase");
        }

        if (StringUtils.isNotBlank(passphraseParam) && StringUtils.isNotBlank(secretPhraseParam)){
            throw new RestParameterException( ApiErrors.ONLY_ONE_OF_PARAM_LIST, "passphrase, secretPhrase");
        }

        String passphrase = null;
        if(StringUtils.isNotBlank(passphraseParam)){
            if (StringUtils.isBlank(accountStr)) {
                throw new RestParameterException( ApiErrors.MISSING_PARAM, "account");
            }
            passphrase = elGamal.elGamalDecrypt(passphraseParam);
        }

        String secretPhrase = null;
        if(StringUtils.isNotBlank(secretPhraseParam)){
            secretPhrase = elGamal.elGamalDecrypt(secretPhraseParam);
        }

        long accountId;

        if (passphrase != null) {
            accountId = Convert.parseAccountId(accountStr);
        } else {
            accountId = Convert.getId(Crypto.getPublicKey(secretPhrase));
        }
        if (accountId == 0) {
            throw new RestParameterException( ApiErrors.INCORRECT_PARAM_VALUE, "account id or passphrase");
        }

        return new TwoFactorAuthParameters(accountId, passphrase, secretPhrase);
    }

    public TwoFactorAuthDetails enable2FA(TwoFactorAuthParameters params2FA){
        TwoFactorAuthDetails authDetails;
        if (params2FA.isPassphrasePresent()) {
            findAplSecretBytes(params2FA.getAccountId(), params2FA.getPassphrase());
        }
        authDetails = service2FA.enable(params2FA.getAccountId());
        return authDetails;
    }

    public Status2FA disable2FA(TwoFactorAuthParameters params2FA, int code){
        Status2FA status2FA;
        if (params2FA.isPassphrasePresent()) {
            findAplSecretBytes(params2FA.getAccountId(), params2FA.getPassphrase());
        }
        status2FA = service2FA.disable(params2FA.getAccountId(), code);
        validate2FAStatus(status2FA, params2FA.getAccountId());
        params2FA.setStatus2FA(status2FA);
        return status2FA;
    }

    public Status2FA disable2FA(long accountId, String passphrase, int code){
        findAplSecretBytes(accountId, passphrase);
        Status2FA status2FA = service2FA.disable(accountId, code);
        validate2FAStatus(status2FA, accountId);
        return status2FA;
    }

    public Status2FA confirm2FA(TwoFactorAuthParameters params2FA, int code) {
        Status2FA status2FA;
        if (params2FA.isPassphrasePresent()) {
            findAplSecretBytes(params2FA.getAccountId(), params2FA.getPassphrase());
        }
        status2FA = service2FA.confirm(params2FA.getAccountId(), code);
        validate2FAStatus(status2FA, params2FA.getAccountId());
        return status2FA;
    }

    public TwoFactorAuthParameters verify2FA(String accountStr, String passphraseParam, String secretPhraseParam, Integer code2FA) throws RestParameterException {
        if (code2FA == null){
            throw new RestParameterException(ApiErrors.MISSING_PARAM, "code2FA");
        }
        TwoFactorAuthParameters params2FA = parse2FARequestParams(accountStr, passphraseParam, secretPhraseParam);
        params2FA.setCode2FA(code2FA);
        if (isEnabled2FA(params2FA.getAccountId())) {
            Status2FA status2FA;
            if (params2FA.isPassphrasePresent()) {
                status2FA = auth2FA(params2FA.getPassphrase(), params2FA.getAccountId(), code2FA);
            } else {
                status2FA = auth2FA(params2FA.getSecretPhrase(), code2FA);
            }
            params2FA.setStatus2FA(status2FA);
            validate2FAStatus(status2FA, params2FA.getAccountId());
        }
        return params2FA;
    }

    public boolean isEnabled2FA(long accountId) {
        return service2FA.isEnabled(accountId);
    }

    public WalletKeysInfo generateUserWallet(String passphrase) throws RestParameterException {
        return generateUserWallet(passphrase, null);
    }

    public WalletKeysInfo generateUserWallet(String passphrase, byte[] secretApl) throws RestParameterException {
        if (passphrase == null) {
            if (passphraseGenerator == null) {
                throw new RestParameterException(ApiErrors.INTERNAL_SERVER_EXCEPTION, "Either passphrase generator or passphrase required");
            }
            passphrase = passphraseGenerator.generate();
        }

        ApolloFbWallet apolloWallet = accountService.generateUserAccounts(secretApl);

        long aplId = apolloWallet.getAplWalletKey().getId();

        KeyStoreService.Status status = keyStoreService.saveSecretKeyStore(passphrase, aplId, apolloWallet);
        validateKeyStoreStatus(aplId, status, "generated");

        WalletKeysInfo walletKeyInfo = new WalletKeysInfo(apolloWallet, passphrase);

        return walletKeyInfo;
    }

    public KeyStoreService.Status deleteAccount(long accountId, String passphrase, int code) throws RestParameterException {
        if (isEnabled2FA(accountId)) {
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
            throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, String.format("%s, account=%d","account id or passphrase", accountId));
        }
        return Convert.parseHexString(fbWallet.getAplKeySecret());
    }

    public byte[] findAplSecretBytes(TwoFactorAuthParameters twoFactorAuthParameters) throws RestParameterException {
        return findAplSecretBytes(twoFactorAuthParameters.getAccountId(), twoFactorAuthParameters.getPassphrase());
    }

    private void validate2FAStatus(Status2FA status2FA, long accountId) throws RestParameterException {
        if (status2FA != Status2FA.OK) {
            log.debug("2fa error: {}-{}", Convert2.rsAccount(accountId), status2FA);
            throw new RestParameterException(ApiErrors.ACCOUNT_2FA_ERROR, String.format("%s, account=%d",status2FA.name(), accountId));
        }
    }

    private Status2FA auth2FA(String passphrase, long accountId, int code) {
        findAplSecretBytes(accountId, passphrase);

        return service2FA.tryAuth(accountId, code);
    }

    private Status2FA auth2FA(String secretPhrase, int code) {
        long accountId = Convert.getId(Crypto.getPublicKey(secretPhrase));
        Status2FA status2FA = service2FA.tryAuth(accountId, code);
        validate2FAStatus(status2FA, accountId);
        return status2FA;
    }

    private void validateKeyStoreStatus(long accountId, KeyStoreService.Status status, String notPerformedAction) {
        if (status != KeyStoreService.Status.OK) {
            log.debug( "Vault wallet not " + notPerformedAction + " {} - {}", Convert2.rsAccount(accountId), status);
            throw new RestParameterException(ApiErrors.ACCOUNT_2FA_ERROR, String.format("Vault wallet for account was not %s : %s", notPerformedAction, status.message));
        }
    }

    // ------ checked to
    @Deprecated
    public WalletKeysInfo importSecretBytes(String passphrase, byte[] secretBytes) throws ParameterException {
        if (passphrase == null) {
            passphrase = passphraseGenerator.generate();
        }
        WalletKeysInfo walletKeysInfo = generateUserWallet(passphrase, secretBytes);
        return walletKeysInfo;
    }
}
