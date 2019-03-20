 /*
  * Copyright © 2018 Apollo Foundation
  */

 package com.apollocurrency.aplwallet.apl.core.app;

 import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
 import com.apollocurrency.aplwallet.apl.crypto.Convert;
 import com.apollocurrency.aplwallet.apl.crypto.Crypto;
 import com.apollocurrency.aplwallet.apl.eth.utils.FbWalletUtil;
 import com.apollocurrency.aplwallet.apl.util.JSON;
 import com.apollocurrency.aplwallet.apl.util.NtpTime;
 import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.fasterxml.jackson.databind.ObjectWriter;
 import io.firstbridge.cryptolib.CryptoNotValidException;
 import io.firstbridge.cryptolib.container.FbWallet;
 import org.slf4j.Logger;

 import javax.enterprise.inject.spi.CDI;
 import javax.inject.Inject;
 import javax.inject.Named;
 import javax.inject.Singleton;
 import java.io.File;
 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.time.Instant;
 import java.time.OffsetDateTime;
 import java.time.ZoneOffset;
 import java.time.format.DateTimeFormatter;
 import java.util.Comparator;
 import java.util.Objects;

 import static org.slf4j.LoggerFactory.getLogger;
@Singleton
 public class VaultKeyStoreImpl implements VaultKeyStore {
     private static final Logger LOG = getLogger(VaultKeyStoreImpl.class);
     private static final byte CURRENT_KEYSTORE_VERSION = 1;
     private Path keystoreDirPath;
     private byte version;
     private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
     private static final String FORMAT = "v%d_%s---%s";
     private NtpTime ntpTime;

    @Inject
    public VaultKeyStoreImpl(@Named("keystoreDirPath")Path keystoreDir) {
        this(keystoreDir, CURRENT_KEYSTORE_VERSION);
    }

     public VaultKeyStoreImpl(Path keystoreDir, byte version) {
         if (version < 0) {
             throw new IllegalArgumentException("version should not be negative");
         }
         this.version = version;
         this.keystoreDirPath = keystoreDir;
         this.ntpTime = CDI.current().select(NtpTime.class).get();
         if (!Files.exists(keystoreDirPath)) {
             try {
                 Files.createDirectories(keystoreDirPath);
             }
             catch (IOException e) {
                 throw new RuntimeException(e.toString(), e);
             }
         }
     }

     private boolean isAvailable() {
         Path path = keystoreDirPath.resolve(".local");
         return !Files.exists(path);
     }

     @Deprecated
     @Override
     public SecretBytesDetails getSecretBytesV0(String passphrase, long accountId)  {
         Objects.requireNonNull(passphrase);

         Path secretPath = findSecretPaths(accountId);

         if (secretPath == null) {
             return new SecretBytesDetails(null, Status.NOT_FOUND);
         }

         try {
             EncryptedSecretBytesDetails secretBytesDetails =
                     JSON.getMapper().readValue(secretPath.toFile(), EncryptedSecretBytesDetails.class);
         byte[] key = Crypto.getKeySeed(passphrase, secretBytesDetails.getNonce(), Convert.longToBytes(secretBytesDetails.getTimestamp()));
             byte[] decryptedSecretBytes = Crypto.aesDecrypt(secretBytesDetails.getEncryptedSecretBytes(), key);

             long actualAccId = Convert.getId(Crypto.getPublicKey(Crypto.getKeySeed(decryptedSecretBytes)));
             if (accountId != actualAccId) {
                 return new SecretBytesDetails(null, Status.BAD_CREDENTIALS);
             }
             return new SecretBytesDetails(decryptedSecretBytes, Status.OK);
         }
         catch (IOException e) {
             return new SecretBytesDetails(null, Status.READ_ERROR);
         }
         catch (RuntimeException e) {
             return new SecretBytesDetails(null, Status.DECRYPTION_ERROR);
         }
     }

     private byte[] generateBytes(int size) {
         byte[] nonce = new byte[size];
         Crypto.getSecureRandom().nextBytes(nonce);
         return nonce;
     }


     public Path findSecretPaths(long accountId) {
         try {
             Path paths = Files.list(keystoreDirPath)
                     //Find files for this account.
                     .filter(path -> Objects.equals(
                             FbWalletUtil.getAccount(path.toString().toUpperCase()),
                             Convert.defaultRsAccount(accountId).toUpperCase())
                     )
                     //Sorted by versions.
                     .sorted(
                             Comparator.comparingInt(o -> FbWalletUtil.getWalletFileVersion(o.toString()))
                                     .reversed()
                     )
                     .findFirst()
                     .orElse(null);

             return paths;
         }
         catch (IOException e) {
             LOG.debug("VaultKeyStore IO error while searching path to secret key of account " + accountId, e.getMessage());
             return null;
         }
     }

    @Override
    public boolean migrateOldKeyStorageToTheNew(String passphrase, long accountId){
        SecretBytesDetails secretBytesDetails = getSecretBytesV0(passphrase, accountId);

        if (secretBytesDetails.getExtractStatus() != Status.OK) {
            return false;
        }
        byte [] secretBytes = secretBytesDetails.getSecretBytes();

        try {
            Helper2FA.generateUserAccounts(passphrase, secretBytes);
        } catch (ParameterException e) {
            LOG.error(e.getMessage(), e);
           return false;
        }

        return true;
     }


     @Override
     @Deprecated
     public Status saveSecretBytes(String passphrase, byte[] secretBytes) {
         if (!isAvailable()) {
             return Status.NOT_AVAILABLE;
         }
         Objects.requireNonNull(passphrase);
         Objects.requireNonNull(secretBytes);
         long accountId = Convert.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretBytes)));
         Path keyPath = makeTargetPathForNewAccount(accountId);
         if (keyPath == null) {
             return Status.DUPLICATE_FOUND;
         }
         EncryptedSecretBytesDetails secretBytesDetails = makeEncryptedSecretBytesDetails(passphrase, secretBytes, accountId);
         boolean saved = storeJSONSecretBytes(keyPath, secretBytesDetails);

         return saved ? Status.OK : Status.WRITE_ERROR;
     }

    @Override
    public Status saveSecretKeyStore(Long accountId, String passphrase, FbWallet fbWallet) {
        byte[] salt = generateBytes(12);
        String path;

        try {
            fbWallet.setOpenData(salt);

            byte[] key = fbWallet.keyFromPassPhrase(passphrase, salt);
            path = makeTargetPathForNewAccount(accountId).toString();

            fbWallet.saveFile(path, key, salt);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return Status.WRITE_ERROR;
        } catch (CryptoNotValidException e) {
            LOG.error(e.getMessage(), e);
            return Status.BAD_CREDENTIALS;
        }

        LOG.info("Created new key store for account " + accountId + ", path - " + path);

        return Status.OK;
    }

    @Override
    public FbWallet getSecretStore(String passphrase, long accountId) {
        Objects.requireNonNull(passphrase);
        Objects.requireNonNull(accountId);

        Path secretPath = findSecretPaths(accountId);

        if (secretPath == null) {
            LOG.warn("VaultWallet : " + Status.NOT_FOUND);
            return null;
        }

        if(FbWalletUtil.getWalletFileVersion(secretPath) != CURRENT_KEYSTORE_VERSION){
            if(migrateOldKeyStorageToTheNew(passphrase, accountId)){
                secretPath = findSecretPaths(accountId);
            }
        }

        FbWallet fbWallet = new FbWallet();
        try {
            fbWallet.openFile(secretPath.toString());
            byte[] salt = fbWallet.getOpenData();
            byte[] key = fbWallet.keyFromPassPhrase(passphrase, salt);

            fbWallet.openFile(secretPath.toString(), key);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        } catch (CryptoNotValidException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }

        return fbWallet;
    }

    @Override
    public File getSecretStoreFile(Long accountId, String passphrase) {
        //TODO check passphrase
        Path secretPath = findSecretPaths(accountId);

        if(FbWalletUtil.getWalletFileVersion(secretPath) != CURRENT_KEYSTORE_VERSION){
            if(migrateOldKeyStorageToTheNew(passphrase, accountId)){
                secretPath = findSecretPaths(accountId);
            }
        }

        return secretPath == null ? null : new File(secretPath.toString());
    }

    @Override
     public Status deleteSecretBytes(String passphrase, long accountId) {
         if (!isAvailable()) {
             return Status.NOT_AVAILABLE;
         }
         SecretBytesDetails secretBytes = getSecretBytesV0(passphrase, accountId);
         if (secretBytes.getExtractStatus() != Status.OK) {
             return secretBytes.getExtractStatus();
         }
         Path secretBytesPath = findSecretPaths(accountId);

        if(FbWalletUtil.getWalletFileVersion(secretBytesPath) != CURRENT_KEYSTORE_VERSION){
            if(migrateOldKeyStorageToTheNew(passphrase, accountId)){
                secretBytesPath = findSecretPaths(accountId);
            }
        }

         return deleteFileWithStatus(secretBytesPath);
     }

     public Status deleteFileWithStatus(Path path) {
         try {
             deleteFile(path);
         }
         catch (IOException e) {
             LOG.debug("Unable to delete file " + path, e);
             return Status.DELETE_ERROR;
         }
         return Status.OK;
     }

     public void deleteFile(Path path) throws IOException {
         Files.delete(path);
     }

     private EncryptedSecretBytesDetails makeEncryptedSecretBytesDetails(String passphrase, byte[] secretBytes, long accountId) {
         byte[] nonce = generateBytes(16);
         long timestamp = ntpTime.getTime();
         byte[] key = Crypto.getKeySeed(passphrase, nonce, Convert.longToBytes(timestamp));
         byte[] encryptedSecretBytes = Crypto.aesEncrypt(secretBytes, key);
         return new EncryptedSecretBytesDetails(encryptedSecretBytes, accountId, version, nonce, timestamp);
     }

     private Path makeTargetPathForNewAccount(long accountId) {
         if (isAccountExist(accountId)) {
             LOG.debug("Account already exist");
             return null;
         }
         return makeTargetPath(accountId);
     }

     private Path makeTargetPath(long accountId) {
         Instant instant = Instant.now();
         OffsetDateTime utcTime = instant.atOffset( ZoneOffset.UTC );
         return keystoreDirPath.resolve(String.format(FORMAT, version, FORMATTER.format(utcTime), Convert.defaultRsAccount(accountId)));
     }

     private boolean isAccountExist(long accountId) {
         return findSecretPaths(accountId) != null;
     }

     public boolean storeJSONSecretBytes(Path keyPath, EncryptedSecretBytesDetails secretBytesDetails) {
         try {
             ObjectMapper mapper = JSON.getMapper();
             ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
             Files.createFile(keyPath);
             writer.writeValue(keyPath.toFile(), secretBytesDetails);
             return true;
         }
         catch (IOException e) {
             LOG.debug("Unable to save secretBytes to " + keyPath, e);
             return false;
         }
     }
 }
