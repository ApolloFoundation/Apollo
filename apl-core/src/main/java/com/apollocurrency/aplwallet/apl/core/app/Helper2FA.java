/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthFileSystemRepository;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthRepositoryImpl;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.http.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.model.AplWalletKey;
import com.apollocurrency.aplwallet.apl.core.model.WalletsInfo;
import com.apollocurrency.aplwallet.apl.core.utils.AccountGeneratorUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.eth.utils.FbWalletUtil;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import io.firstbridge.cryptolib.container.FbWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

/**
 * This class is just static helper for 2FA. It should be removed later
 * and replaced by properly used CDI
 * @author al
 */
public class Helper2FA {
   private static TwoFactorAuthService service2FA;
   private static final Logger LOG = LoggerFactory.getLogger(Helper2FA.class);
   private static final PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
   private static final VaultKeyStore KEYSTORE = CDI.current().select(VaultKeyStore.class).get();
   private static final PassphraseGeneratorImpl passphraseGenerator = new PassphraseGeneratorImpl(10, 15);

     public static void init(DatabaseManager databaseManagerParam) {
        DatabaseManager databaseManager = databaseManagerParam;
        service2FA = new TwoFactorAuthServiceImpl(
                propertiesHolder.getBooleanProperty("apl.store2FAInFileSystem")
                        ? new TwoFactorAuthFileSystemRepository(AplCoreRuntime.getInstance().get2FADir())
                        : new TwoFactorAuthRepositoryImpl(databaseManager.getDataSource()),
                propertiesHolder.getStringProperty("apl.issuerSuffix2FA", RuntimeEnvironment.getInstance().isDesktopApplicationEnabled() ? "desktop" : "web"));
    }
     
    public static TwoFactorAuthDetails enable2FA(long accountId, String passphrase) throws ParameterException {
            findAplSecretBytes(accountId, passphrase, true);
            return service2FA.enable(accountId);
    }
    public static TwoFactorAuthDetails enable2FA(String secretPhrase) throws ParameterException {
        return service2FA.enable(Convert.getId(Crypto.getPublicKey(secretPhrase)));
    }


    public static Status2FA disable2FA(long accountId, String passphrase, int code) throws ParameterException {
        findAplSecretBytes(accountId, passphrase, true);
        Status2FA status2FA = service2FA.disable(accountId, code);
        validate2FAStatus(status2FA, accountId);
        return status2FA;
    }


    public static Status2FA disable2FA(String secretPhrase, int code) throws ParameterException {
        long id = Convert.getId(Crypto.getPublicKey(secretPhrase));
        Status2FA status2FA = service2FA.disable(id, code);
        validate2FAStatus(status2FA, id);
        return status2FA;
    }

    public static boolean isEnabled2FA(long accountId) {
        return service2FA.isEnabled(accountId);
    }


    public static void verify2FA(HttpServletRequest req, String accountName) throws ParameterException {
        TwoFactorAuthParameters params2FA = ParameterParser.parse2FARequest(req, accountName, false);

        if (isEnabled2FA(params2FA.getAccountId())) {
            TwoFactorAuthParameters.requireSecretPhraseOrPassphrase(params2FA);
            int code = ParameterParser.getInt(req,"code2FA", Integer.MIN_VALUE, Integer.MAX_VALUE, true);
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

    public static byte[] findAplSecretBytes(long accountId, String passphrase, boolean isMandatory) throws ParameterException {
        FbWallet fbWallet = KEYSTORE.getSecretStore(passphrase, accountId);
        String secret = FbWalletUtil.getAplKeySecret(fbWallet);

        return Convert.parseHexString(secret);
    }

    public static VaultKeyStore.Status deleteAccount(long accountId, String passphrase, int code) throws ParameterException {
        if (isEnabled2FA(accountId)) {
            Status2FA status2FA = disable2FA(accountId, passphrase, code);
            validate2FAStatus(status2FA, accountId);
        }
        VaultKeyStore.Status status = KEYSTORE.deleteKeyStore(passphrase, accountId);
        validateKeyStoreStatus(accountId, status, "deleted");
        return status;
    }

    public static Status2FA confirm2FA(long accountId, String passphrase, int code) throws ParameterException {
        findAplSecretBytes(accountId, passphrase, true);
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
    }

    public static Status2FA auth2FA(String passphrase, long accountId, int code) throws ParameterException {
        byte[] secretBytes = findAplSecretBytes(accountId, passphrase, true);
        return service2FA.tryAuth(accountId, code);
    }

    public static Status2FA auth2FA(String secretPhrase, int code) throws ParameterException {
        long accountId = Convert.getId(Crypto.getPublicKey(secretPhrase));
        Status2FA status2FA = service2FA.tryAuth(accountId, code);
        validate2FAStatus(status2FA, accountId);
        return status2FA;
    }

    public static WalletsInfo generateUserAccounts(String passphrase) throws ParameterException {
         return generateUserAccounts(passphrase, null);
    }

    public static WalletsInfo generateUserAccounts(String passphrase, byte[] secretApl) throws ParameterException {
        if (passphrase == null) {
            if (passphraseGenerator == null) {
                throw new RuntimeException("Either passphrase generator or passphrase required");
            }
            passphrase = passphraseGenerator.generate();
        }

        FbWallet fbWallet = new FbWallet();
        AplWalletKey aplAccount = secretApl == null ? AccountGeneratorUtil.generateApl() : AccountGeneratorUtil.generateApl(secretApl);
        EthWalletKey ethAccount = AccountGeneratorUtil.generateEth();

        FbWalletUtil.addAplKey(aplAccount, fbWallet);
        FbWalletUtil.addEthKey(ethAccount, fbWallet);
        //throw Exception if OpenData null
        fbWallet.setOpenData(new byte[1]);

        VaultKeyStore.Status status = KEYSTORE.saveSecretKeyStore(aplAccount.getId(), passphrase, fbWallet);
        validateKeyStoreStatus(aplAccount.getId(), status, "generated");

        WalletsInfo walletKeyInfo = new WalletsInfo(ethAccount, aplAccount, passphrase);
        return walletKeyInfo;
    }

    private static void validateKeyStoreStatus(long accountId, VaultKeyStore.Status status, String notPerformedAction) throws ParameterException {
        if (status != VaultKeyStore.Status.OK) {
            LOG.debug( "Vault wallet not " + notPerformedAction + " {} - {}", Convert2.rsAccount(accountId), status);
            throw new ParameterException("Unable to generate account", null, JSONResponses.vaultWalletError(accountId, notPerformedAction,
                    status.message));
        }
    }

    @Deprecated
    public static WalletsInfo importSecretBytes(String passphrase, byte[] secretBytes) throws ParameterException {
        if (passphrase == null) {
            passphrase = passphraseGenerator.generate();
        }
        WalletsInfo walletsInfo = generateUserAccounts(passphrase, secretBytes);
        return walletsInfo;
    }


    
}
