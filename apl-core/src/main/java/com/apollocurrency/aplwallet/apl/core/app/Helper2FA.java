/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.apl.core.model.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.service.appdata.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TwoFactorAuthService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TwoFactorAuthServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.PassphraseGeneratorImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.TwoFactorAuthFileSystemRepository;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.TwoFactorAuthRepositoryImpl;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.apl.core.model.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

/**
 * This class is just static helper for 2FA. It should be removed later
 * and replaced by properly used CDI
 *
 * @author al
 * @deprecated Use {@link com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper} class instead of this one.
 */
@Slf4j
@Deprecated
public class Helper2FA {
    private static final Logger LOG = LoggerFactory.getLogger(Helper2FA.class);
    private static final PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    private static final DirProvider dirProvider = CDI.current().select(DirProvider.class).get();
    private static final KeyStoreService KEYSTORE = CDI.current().select(KeyStoreService.class).get();
    private static final AccountService accountService = CDI.current().select(AccountService.class).get();
    private static final PassphraseGeneratorImpl passphraseGenerator = new PassphraseGeneratorImpl(10, 15);
    private static TwoFactorAuthService service2FA;
    private static boolean is2FaInFile;

    public static void init(DatabaseManager databaseManagerParam) {
        is2FaInFile = propertiesHolder.getBooleanProperty("apl.store2FAInFileSystem", false);
        log.trace("is2FaInFile = {}", is2FaInFile);
        service2FA = new TwoFactorAuthServiceImpl(
            new TwoFactorAuthRepositoryImpl(databaseManagerParam.getDataSource()),
            propertiesHolder.getStringProperty("apl.issuerSuffix2FA",
                RuntimeEnvironment.getInstance().isDesktopApplicationEnabled() ? "desktop" : "web"),
            new TwoFactorAuthFileSystemRepository(dirProvider.get2FADir())
        );
    }

    public static void attemptMoveDataFromDatabase() {
        // move data from db into file
        if (!is2FaInFile) {
            log.trace("try move data from db into file...");
            service2FA.attemptMoveDataFromDatabase();
        }
    }

    public static TwoFactorAuthDetails enable2FA(long accountId, String passphrase) throws ParameterException {
        findAplSecretBytes(accountId, passphrase);
        TwoFactorAuthDetails details = service2FA.enable(accountId);
        log.trace("enable2FA, accountId = {}, res = {}", accountId, details);
        return details;
    }

    public static TwoFactorAuthDetails enable2FA(String secretPhrase) throws ParameterException {
        return service2FA.enable(Convert.getId(Crypto.getPublicKey(secretPhrase)));
    }

    public static Status2FA disable2FA(long accountId, String passphrase, int code) throws ParameterException {
        findAplSecretBytes(accountId, passphrase);
        Status2FA status2FA = service2FA.disable(accountId, code);
        validate2FAStatus(status2FA, accountId);
        log.trace("disable2FA, accountId = {}, res = {}", accountId, status2FA);
        return status2FA;
    }


    public static Status2FA disable2FA(String secretPhrase, int code) throws ParameterException {
        long id = Convert.getId(Crypto.getPublicKey(secretPhrase));
        Status2FA status2FA = service2FA.disable(id, code);
        validate2FAStatus(status2FA, id);
        log.trace("disable2FA, code = {}, res = {}", code, status2FA);
        return status2FA;
    }

    public static boolean isEnabled2FA(long accountId) {
        boolean details = service2FA.isEnabled(accountId);
        log.trace("isEnabled2FA, accountId = {}, res = {}", accountId, details);
        return details;
    }


    public static void verify2FA(HttpServletRequest req, String accountName) throws ParameterException {
        TwoFactorAuthParameters params2FA = HttpParameterParserUtil.parse2FARequest(req, accountName, false);

        if (isEnabled2FA(params2FA.getAccountId())) {
            requireSecretPhraseOrPassphrase(params2FA);
            int code = HttpParameterParserUtil.getInt(req, "code2FA", Integer.MIN_VALUE, Integer.MAX_VALUE, true);
            Status2FA status2FA;
            long accountId;
            if (params2FA.isPassphrasePresent()) {
                status2FA = auth2FA(params2FA.getPassphrase(), params2FA.getAccountId(), code);
                accountId = params2FA.getAccountId();
            } else {
                status2FA = auth2FA(params2FA.getSecretPhrase(), code);
                accountId = Convert.getId(Crypto.getPublicKey(params2FA.getSecretPhrase()));
            }
            validate2FAStatus(status2FA, accountId);
        }
    }

    public static void requireSecretPhraseOrPassphrase(TwoFactorAuthParameters params2FA) throws ParameterException {
        if (!params2FA.isPassphrasePresent() && !params2FA.isSecretPhrasePresent()) {
            throw new ParameterException(JSONResponses.either("secretPhrase", "passphrase"));
        }
    }

    public static void verifyVault2FA(long accountId, int code2FA) throws ParameterException {

        if (isEnabled2FA(accountId)) {
            Status2FA status2FA = service2FA.tryAuth(accountId, code2FA);
            validate2FAStatus(status2FA, accountId);
        }
    }

    public static byte[] findAplSecretBytes(long accountId, String passphrase) throws ParameterException {
        ApolloFbWallet fbWallet = KEYSTORE.getSecretStore(passphrase, accountId);

        if (fbWallet == null) {
            throw new ParameterException(JSONResponses.incorrect("account id or passphrase"));
        }

        return Convert.parseHexString(fbWallet.getAplKeySecret());
    }

    public static KeyStoreService.Status deleteAccount(long accountId, String passphrase, int code) throws ParameterException {
        if (isEnabled2FA(accountId)) {
            Status2FA status2FA = disable2FA(accountId, passphrase, code);
            validate2FAStatus(status2FA, accountId);
        }
        KeyStoreService.Status status = KEYSTORE.deleteKeyStore(passphrase, accountId);
        validateKeyStoreStatus(accountId, status, "deleted");
        return status;
    }

    public static Status2FA confirm2FA(long accountId, String passphrase, int code) throws ParameterException {
        findAplSecretBytes(accountId, passphrase);
        Status2FA status2FA = service2FA.confirm(accountId, code);
        validate2FAStatus(status2FA, accountId);
        return status2FA;
    }

    public static Status2FA confirm2FA(String secretPhrase, int code) throws ParameterException {
        long accountId = Convert.getId(Crypto.getPublicKey(secretPhrase));
        Status2FA status2FA = service2FA.confirm(accountId, code);
        validate2FAStatus(status2FA, accountId);
        return status2FA;
    }

    private static void validate2FAStatus(Status2FA status2FA, long account) throws ParameterException {
        if (status2FA != Status2FA.OK) {
            LOG.debug("2fa error: {}-{}", Convert2.rsAccount(account), status2FA);
            throw new ParameterException("2fa error", null, JSONResponses.error2FA(status2FA, account));
        }
        log.trace("validate2FAStatus, account = {}, res = {}", account, status2FA);
    }

    public static Status2FA auth2FA(String passphrase, long accountId, int code) throws ParameterException {
        findAplSecretBytes(accountId, passphrase);
        Status2FA status2FA = service2FA.tryAuth(accountId, code);
        log.trace("auth2FA, accountId = {}, res = {}", accountId, status2FA);
        return status2FA;
    }

    public static Status2FA auth2FA(String secretPhrase, int code) throws ParameterException {
        long accountId = Convert.getId(Crypto.getPublicKey(secretPhrase));
        Status2FA status2FA = service2FA.tryAuth(accountId, code);
        validate2FAStatus(status2FA, accountId);
        return status2FA;
    }

    public static WalletKeysInfo generateUserWallet(String passphrase) throws ParameterException {
        return generateUserWallet(passphrase, null);
    }

    public static WalletKeysInfo generateUserWallet(String passphrase, byte[] secretApl) throws ParameterException {
        if (passphrase == null) {
            if (passphraseGenerator == null) {
                throw new RuntimeException("Either passphrase generator or passphrase required");
            }
            passphrase = passphraseGenerator.generate();
        }

        ApolloFbWallet apolloWallet = accountService.generateUserAccounts(secretApl);

        long aplId = apolloWallet.getAplWalletKey().getId();

        KeyStoreService.Status status = KEYSTORE.saveSecretKeyStore(passphrase, aplId, apolloWallet);
        validateKeyStoreStatus(aplId, status, "generated");

        WalletKeysInfo walletKeyInfo = new WalletKeysInfo(apolloWallet, passphrase);

        return walletKeyInfo;
    }

    private static void validateKeyStoreStatus(long accountId, KeyStoreService.Status status, String notPerformedAction) throws ParameterException {
        if (status != KeyStoreService.Status.OK) {
            LOG.debug("Vault wallet not " + notPerformedAction + " {} - {}", Convert2.rsAccount(accountId), status);
            throw new ParameterException("Unable to generate account", null, JSONResponses.vaultWalletError(accountId, notPerformedAction,
                status.message));
        }
    }

    @Deprecated
    public static WalletKeysInfo importSecretBytes(String passphrase, byte[] secretBytes) throws ParameterException {
        if (passphrase == null) {
            passphrase = passphraseGenerator.generate();
        }
        WalletKeysInfo walletKeysInfo = generateUserWallet(passphrase, secretBytes);
        return walletKeysInfo;
    }


}
