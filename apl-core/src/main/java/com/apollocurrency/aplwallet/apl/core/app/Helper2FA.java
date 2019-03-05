/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.apl.core.account.AccountGenerator;
import com.apollocurrency.aplwallet.apl.core.account.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.LegacyAccountGenerator;
import com.apollocurrency.aplwallet.apl.core.app.PassphraseGeneratorImpl;
import com.apollocurrency.aplwallet.apl.core.app.SecretBytesDetails;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthService;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.VaultKeyStore;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthFileSystemRepository;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthRepositoryImpl;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.http.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   private static final AccountGenerator accountGenerator = new LegacyAccountGenerator(passphraseGenerator); 
    
     public static void init(DatabaseManager databaseManagerParam) {
        DatabaseManager databaseManager = databaseManagerParam;
        service2FA = new TwoFactorAuthServiceImpl(
                propertiesHolder.getBooleanProperty("apl.store2FAInFileSystem")
                        ? new TwoFactorAuthFileSystemRepository(AplCoreRuntime.getInstance().get2FADir())
                        : new TwoFactorAuthRepositoryImpl(databaseManager.getDataSource()),
                propertiesHolder.getStringProperty("apl.issuerSuffix2FA", RuntimeEnvironment.getInstance().isDesktopApplicationEnabled() ? "desktop" : "web"));
    }
     
    public static TwoFactorAuthDetails enable2FA(long accountId, String passphrase) throws ParameterException {
            findSecretBytes(accountId, passphrase, true);
            return service2FA.enable(accountId);
    }
    public static TwoFactorAuthDetails enable2FA(String secretPhrase) throws ParameterException {
        return service2FA.enable(Convert.getId(Crypto.getPublicKey(secretPhrase)));
    }


    public static Status2FA disable2FA(long accountId, String passphrase, int code) throws ParameterException {
        findSecretBytes(accountId, passphrase, true);
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

    public static SecretBytesDetails findSecretBytes(long accountId, String passphrase, boolean isMandatory) throws ParameterException {
        SecretBytesDetails secretBytes = KEYSTORE.getSecretBytes(passphrase, accountId);

        if (isMandatory) {
            validateKeyStoreStatus(accountId, secretBytes.getExtractStatus(), "found");
        }

        return secretBytes;
    }

    public static VaultKeyStore.Status deleteAccount(long accountId, String passphrase, int code) throws ParameterException {
        if (isEnabled2FA(accountId)) {
            Status2FA status2FA = disable2FA(accountId, passphrase, code);
            validate2FAStatus(status2FA, accountId);
        }
        VaultKeyStore.Status status = KEYSTORE.deleteSecretBytes(passphrase, accountId);
        validateKeyStoreStatus(accountId, status, "deleted");
        return status;
    }

    public static Status2FA confirm2FA(long accountId, String passphrase, int code) throws ParameterException {
        findSecretBytes(accountId, passphrase, true);
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
        SecretBytesDetails secretBytes = findSecretBytes(accountId, passphrase, true);
        return service2FA.tryAuth(accountId, code);
    }

    public static Status2FA auth2FA(String secretPhrase, int code) throws ParameterException {
        long accountId = Convert.getId(Crypto.getPublicKey(secretPhrase));
        Status2FA status2FA = service2FA.tryAuth(accountId, code);
        validate2FAStatus(status2FA, accountId);
        return status2FA;
    }
    public static GeneratedAccount generateAccount(String passphrase) throws ParameterException {
        GeneratedAccount account = accountGenerator.generate(passphrase);
        VaultKeyStore.Status status = KEYSTORE.saveSecretBytes(account.getPassphrase(), account.getSecretBytes());
        validateKeyStoreStatus(account.getId(), status, "generated");
        return account;
    }

    private static void validateKeyStoreStatus(long accountId, VaultKeyStore.Status status, String notPerformedAction) throws ParameterException {
        if (status != VaultKeyStore.Status.OK) {
            LOG.debug( "Vault wallet not " + notPerformedAction + " {} - {}", Convert2.rsAccount(accountId), status);
            throw new ParameterException("Unable to generate account", null, JSONResponses.vaultWalletError(accountId, notPerformedAction,
                    status.message));
        }
    }

    public static byte[] exportSecretBytes(String passphrase, long accountId) throws ParameterException {
        return findSecretBytes(accountId, passphrase, true).getSecretBytes();
    }
    public static Pair<VaultKeyStore.Status, String> importSecretBytes(String passphrase, byte[] secretBytes) throws ParameterException {
        if (passphrase == null) {
            passphrase = passphraseGenerator.generate();
        }
        VaultKeyStore.Status status = KEYSTORE.saveSecretBytes(passphrase, secretBytes);
        long accountId = Convert.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretBytes)));
        validateKeyStoreStatus(accountId, status, "imported");
        return new ImmutablePair<>(status, passphrase);
    }


    
}
