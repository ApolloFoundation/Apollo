 /*
  * Copyright © 2018 Apollo Foundation
  */

 package com.apollocurrency.aplwallet.apl.core.service.appdata.impl;

 import com.apollocurrency.aplwallet.apl.core.app.EncryptedSecretBytesDetails;
 import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
 import com.apollocurrency.aplwallet.apl.core.app.SecretBytesDetails;
 import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
 import com.apollocurrency.aplwallet.apl.core.model.AplWalletKey;
 import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
 import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
 import com.apollocurrency.aplwallet.apl.core.service.appdata.KeyStoreService;
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
 import org.apache.commons.collections4.CollectionUtils;
 import org.slf4j.Logger;

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
 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.List;
 import java.util.Objects;
 import java.util.stream.Collectors;
 import java.util.stream.Stream;

 import static org.slf4j.LoggerFactory.getLogger;

 @Singleton
 public class VaultKeyStoreServiceImpl implements KeyStoreService {
     private static final Logger LOG = getLogger(VaultKeyStoreServiceImpl.class);
     private static final Integer CURRENT_KEYSTORE_VERSION = 1;
     private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
     private static final String FORMAT = "v%d_%s---%s";
     private Path keystoreDirPath;
     private Integer version;
     private NtpTime ntpTime;

     @Inject
     public VaultKeyStoreServiceImpl(@Named("keystoreDirPath") Path keystoreDir, NtpTime ntpTime) {
         this(keystoreDir, CURRENT_KEYSTORE_VERSION, ntpTime);
     }

     public VaultKeyStoreServiceImpl(Path keystoreDir, Integer version, NtpTime ntpTime) {
         if (version < 0) {
             throw new IllegalArgumentException("version should not be negative");
         }
         this.version = version;
         this.keystoreDirPath = keystoreDir;
         this.ntpTime = ntpTime;
         if (!Files.exists(keystoreDirPath)) {
             try {
                 Files.createDirectories(keystoreDirPath);
             } catch (IOException e) {
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
     public SecretBytesDetails getSecretBytesV0(String passphrase, long accountId) {
         Objects.requireNonNull(passphrase);

         Path secretPath = findKeyStorePathWithLatestVersion(accountId);

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
         } catch (IOException e) {
             return new SecretBytesDetails(null, Status.READ_ERROR);
         } catch (RuntimeException e) {
             return new SecretBytesDetails(null, Status.DECRYPTION_ERROR);
         }
     }

     private byte[] generateBytes(int size) {
         byte[] nonce = new byte[size];
         Crypto.getSecureRandom().nextBytes(nonce);
         return nonce;
     }


     public Path findKeyStorePathWithLatestVersion(long accountId) {
         try (Stream<Path> files = Files.list(keystoreDirPath)) {
             Path path = files
                 //Find files for this account.
                 .filter(p -> Objects.equals(
                     FbWalletUtil.getAccount(p.toString().toUpperCase()),
                     Convert.defaultRsAccount(accountId).toUpperCase())
                 )
                 //Sorted by versions.
                 .sorted(
                     Comparator.comparingInt(o -> FbWalletUtil.getWalletFileVersion(o.toString()))
                         .reversed()
                 )
                 .findFirst()
                 .orElse(null);

             return path;
         } catch (IOException e) {
             LOG.debug("VaultKeyStore IO error while searching path to secret key of account " + accountId, e.getMessage());
             return null;
         }
     }

     public List<Path> findKeyStorePaths(long accountId) {
         try (Stream<Path> files = Files.list(keystoreDirPath)) {
             List<Path> paths = files
                 //Find files for this account.
                 .filter(path -> Objects.equals(
                     FbWalletUtil.getAccount(path.toString().toUpperCase()),
                     Convert.defaultRsAccount(accountId).toUpperCase())
                 )
                 .collect(Collectors.toList());
             return paths;
         } catch (IOException e) {
             LOG.debug("VaultKeyStore IO error while searching path to secret key of account " + accountId, e.getMessage());
             return new ArrayList<>();
         }
     }

     @Override
     public boolean migrateOldKeyStorageToTheNew(String passphrase, long accountId) {
         SecretBytesDetails secretBytesDetails = getSecretBytesV0(passphrase, accountId);

         if (secretBytesDetails.getExtractStatus() != Status.OK) {
             return false;
         }
         byte[] secretBytes = secretBytesDetails.getSecretBytes();

         try {
             Helper2FA.generateUserWallet(passphrase, secretBytes);
         } catch (ParameterException e) {
             LOG.error(e.getMessage(), e);
             return false;
         }

         return true;
     }


     @Deprecated
     @Override
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

     public Status saveSecretKeyStore(String passphrase, ApolloFbWallet fbWallet) {
         String aplKeySecret = fbWallet.getAplKeySecret();

         AplWalletKey aplWalletKey = new AplWalletKey(Convert.parseHexString(aplKeySecret));

         return saveSecretKeyStore(passphrase, aplWalletKey.getId(), fbWallet);
     }

     @Override
     public Status saveSecretKeyStore(String passphrase, Long accountId, FbWallet fbWallet) {
         byte[] salt = generateBytes(12);
         Path path;

         try {
             if (isNewVersionOfKeyStoreForAccountExist(accountId)) {
                 return Status.DUPLICATE_FOUND;
             }

             byte[] key = fbWallet.keyFromPassPhrase(passphrase, salt);
             path = makeTargetPathForNewAccount(accountId);

             if (path == null) {
                 return Status.BAD_CREDENTIALS;
             }

             fbWallet.saveFile(path.toString(), key, salt);
         } catch (IOException e) {
             LOG.error(e.getMessage(), e);
             return Status.WRITE_ERROR;
         } catch (CryptoNotValidException e) {
             LOG.error(e.getMessage(), e);
             return Status.BAD_CREDENTIALS;
         }

         LOG.info("Created new key store for account " + accountId + ", path - " + path.toString());

         return Status.OK;
     }

     @Override
     public ApolloFbWallet getSecretStore(String passphrase, long accountId) {
         Objects.requireNonNull(passphrase);

         Path secretPath = findKeyStorePathWithLatestVersion(accountId);

         if (secretPath == null) {
             LOG.warn("VaultWallet : " + Status.NOT_FOUND);
             return null;
         }

         if (!isStorageVersionLatest(secretPath)) {
             if (migrateOldKeyStorageToTheNew(passphrase, accountId)) {
                 secretPath = findKeyStorePathWithLatestVersion(accountId);
             }
         }

         ApolloFbWallet fbWallet = new ApolloFbWallet();
         try {
             fbWallet.readOpenData(secretPath.toString());
             byte[] salt = fbWallet.getContanerIV();
             byte[] key = fbWallet.keyFromPassPhrase(passphrase, salt);

             fbWallet.openFile(secretPath.toString(), key);
         } catch (IOException | CryptoNotValidException e) {
             LOG.error(e.getMessage(), e);
             return null;
         }

         return fbWallet;
     }

     @Override
     public WalletKeysInfo getWalletKeysInfo(String passphrase, long accountId) {
         ApolloFbWallet fbWallet = getSecretStore(passphrase, accountId);

         if (fbWallet == null) {
             LOG.warn("VaultWallet : " + Status.NOT_FOUND);
             return null;
         }
         return new WalletKeysInfo(fbWallet, passphrase);
     }

     @Override
     public File getSecretStoreFile(Long accountId, String passphrase) {
         Path secretPath = findKeyStorePathWithLatestVersion(accountId);

         // Check passphrase / migrate keys from old key store to the new.
         FbWallet fbWallet = getSecretStore(passphrase, accountId);

         if (fbWallet == null || CollectionUtils.isEmpty(fbWallet.getAllKeys())) {
             return null;
         }

         return secretPath == null ? null : new File(secretPath.toString());
     }

     @Override
     public Status deleteKeyStore(String passphrase, long accountId) {
         if (!isAvailable()) {
             return Status.NOT_AVAILABLE;
         }

         FbWallet fbWallet = getSecretStore(passphrase, accountId);
         if (fbWallet == null || CollectionUtils.isEmpty(fbWallet.getAllData())) {
             return Status.BAD_CREDENTIALS;
         }
         List<Path> secretPaths = findKeyStorePaths(accountId);

         return deleteFileWithStatus(secretPaths);
     }

     public Status deleteFileWithStatus(List<Path> paths) {
         try {
             for (Path path : paths) {
                 deleteFile(path);
             }
         } catch (IOException e) {
             LOG.debug("Unable to delete file. " + paths.get(0), e);
             return Status.DELETE_ERROR;
         }
         return Status.OK;
     }

     public void deleteFile(Path path) throws IOException {
         Files.delete(path);
     }

     @Deprecated
     private EncryptedSecretBytesDetails makeEncryptedSecretBytesDetails(String passphrase, byte[] secretBytes, long accountId) {
         byte[] nonce = generateBytes(16);
         long timestamp = ntpTime.getTime();
         byte[] key = Crypto.getKeySeed(passphrase, nonce, Convert.longToBytes(timestamp));
         byte[] encryptedSecretBytes = Crypto.aesEncrypt(secretBytes, key);
         return new EncryptedSecretBytesDetails(encryptedSecretBytes, accountId, version, nonce, timestamp);
     }

     private Path makeTargetPathForNewAccount(long accountId) {
         if (isNewVersionOfKeyStoreForAccountExist(accountId)) {
             LOG.debug("Account already exist");
             return null;
         }
         return makeTargetPath(accountId);
     }

     private Path makeTargetPath(long accountId) {
         Instant instant = Instant.now();
         OffsetDateTime utcTime = instant.atOffset(ZoneOffset.UTC);
         return keystoreDirPath.resolve(String.format(FORMAT, version, FORMATTER.format(utcTime), Convert.defaultRsAccount(accountId)));
     }

     public boolean isNewVersionOfKeyStoreForAccountExist(long accountId) {
         Path path = findKeyStorePathWithLatestVersion(accountId);

         return path != null && isStorageVersionLatest(path);
     }

     public boolean isKeyStoreForAccountExist(long accountId) {
         Path path = findKeyStorePathWithLatestVersion(accountId);

         return path != null;
     }

     public boolean storeJSONSecretBytes(Path keyPath, EncryptedSecretBytesDetails secretBytesDetails) {
         try {
             ObjectMapper mapper = JSON.getMapper();
             ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
             Files.createFile(keyPath);
             writer.writeValue(keyPath.toFile(), secretBytesDetails);
             return true;
         } catch (IOException e) {
             LOG.debug("Unable to save secretBytes to " + keyPath, e);
             return false;
         }
     }


     private boolean isStorageVersionLatest(Path path) {
         return CURRENT_KEYSTORE_VERSION.equals(FbWalletUtil.getWalletFileVersion(path));
     }
 }
