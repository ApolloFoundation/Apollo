 /*
  * Copyright © 2018 Apollo Foundation
  */

 package com.apollocurrency.aplwallet.vault;

 import com.apollocurrency.aplwallet.apl.crypto.Convert;
 import com.apollocurrency.aplwallet.apl.crypto.Crypto;
 import com.apollocurrency.aplwallet.apl.util.AplCollectionUtils;
 import com.apollocurrency.aplwallet.apl.util.Convert2;
 import com.apollocurrency.aplwallet.apl.util.JSON;
 import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;
 import com.apollocurrency.aplwallet.vault.model.AplWalletKey;
 import com.apollocurrency.aplwallet.vault.model.ApolloFbWallet;
 import com.apollocurrency.aplwallet.vault.model.EncryptedSecretBytesDetails;
 import com.apollocurrency.aplwallet.vault.model.KMSResponseStatus;
 import com.apollocurrency.aplwallet.vault.model.SecretBytesDetails;
 import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;
 import com.apollocurrency.aplwallet.vault.util.AccountHelper;
 import com.apollocurrency.aplwallet.vault.util.FbWalletUtil;
 import io.firstbridge.cryptolib.CryptoNotValidException;
 import io.firstbridge.cryptolib.container.FbWallet;
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

     @Inject
     public VaultKeyStoreServiceImpl(@Named("keystoreDirPath") Path keystoreDir) {
         this(keystoreDir, CURRENT_KEYSTORE_VERSION);
     }

     VaultKeyStoreServiceImpl(Path keystoreDir, Integer version) {
         if (version < 0) {
             throw new IllegalArgumentException("version should not be negative");
         }
         this.version = version;
         this.keystoreDirPath = keystoreDir;
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
             return new SecretBytesDetails(null, KMSResponseStatus.NOT_FOUND);
         }

         try {
             EncryptedSecretBytesDetails secretBytesDetails =
                 JSON.getMapper().readValue(secretPath.toFile(), EncryptedSecretBytesDetails.class);
             byte[] key = Crypto.getKeySeed(passphrase, secretBytesDetails.getNonce(), Convert.longToBytes(secretBytesDetails.getTimestamp()));
             byte[] decryptedSecretBytes = Crypto.aesDecrypt(secretBytesDetails.getEncryptedSecretBytes(), key);

             long actualAccId = Convert.getId(Crypto.getPublicKey(Crypto.getKeySeed(decryptedSecretBytes)));
             if (accountId != actualAccId) {
                 return new SecretBytesDetails(null, KMSResponseStatus.BAD_CREDENTIALS);
             }
             return new SecretBytesDetails(decryptedSecretBytes, KMSResponseStatus.OK);
         } catch (IOException e) {
             return new SecretBytesDetails(null, KMSResponseStatus.READ_ERROR);
         } catch (RuntimeException e) {
             return new SecretBytesDetails(null, KMSResponseStatus.DECRYPTION_ERROR);
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
                     Convert2.defaultRsAccount(accountId).toUpperCase())
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
                     Convert2.defaultRsAccount(accountId).toUpperCase())
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

         if (secretBytesDetails.getExtractStatus() != KMSResponseStatus.OK) {
             return false;
         }
         byte[] secretBytes = secretBytesDetails.getSecretBytes();

         try {
             ApolloFbWallet apolloWallet = AccountHelper.generateApolloWallet(secretBytes);
             KMSResponseStatus status = saveSecretKeyStore(passphrase, apolloWallet);

             if (status != KMSResponseStatus.OK) {
                 return false;
             }
         } catch (RestParameterException e) {
             LOG.error(e.getMessage(), e);
             return false;
         }
         return true;
     }


     public KMSResponseStatus saveSecretKeyStore(String passphrase, ApolloFbWallet fbWallet) {
         String aplKeySecret = fbWallet.getAplKeySecret();

         AplWalletKey aplWalletKey = new AplWalletKey(Convert.parseHexString(aplKeySecret));

         return saveSecretKeyStore(passphrase, aplWalletKey.getId(), fbWallet);
     }

     @Override
     public KMSResponseStatus saveSecretKeyStore(String passphrase, Long accountId, FbWallet fbWallet) {
         byte[] salt = generateBytes(12);
         Path path;

         try {
             if (isNewVersionOfKeyStoreForAccountExist(accountId)) {
                 return KMSResponseStatus.DUPLICATE_FOUND;
             }

             byte[] key = fbWallet.keyFromPassPhrase(passphrase, salt);
             path = makeTargetPathForNewAccount(accountId);

             if (path == null) {
                 return KMSResponseStatus.BAD_CREDENTIALS;
             }

             fbWallet.saveFile(path.toString(), key, salt);
         } catch (IOException e) {
             LOG.error(e.getMessage(), e);
             return KMSResponseStatus.WRITE_ERROR;
         } catch (CryptoNotValidException e) {
             LOG.error(e.getMessage(), e);
             return KMSResponseStatus.BAD_CREDENTIALS;
         }

         LOG.info("Created new key store for account " + accountId + ", path - " + path.toString());

         return KMSResponseStatus.OK;
     }

     @Override
     public ApolloFbWallet getSecretStore(String passphrase, long accountId) {
         Objects.requireNonNull(passphrase);

         Path secretPath = findKeyStorePathWithLatestVersion(accountId);

         if (secretPath == null) {
             LOG.warn("VaultWallet : " + KMSResponseStatus.NOT_FOUND);
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
             byte[] salt = fbWallet.getContainerIV();
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
             LOG.warn("VaultWallet : " + KMSResponseStatus.NOT_FOUND);
             return null;
         }
         return new WalletKeysInfo(fbWallet, passphrase);
     }

     @Override
     public File getSecretStoreFile(Long accountId, String passphrase) {
         Path secretPath = findKeyStorePathWithLatestVersion(accountId);

         // Check passphrase / migrate keys from old key store to the new.
         FbWallet fbWallet = getSecretStore(passphrase, accountId);

         if (fbWallet == null || AplCollectionUtils.isEmpty(fbWallet.getAllKeys())) {
             return null;
         }

         return secretPath == null ? null : new File(secretPath.toString());
     }

     @Override
     public KMSResponseStatus deleteKeyStore(String passphrase, long accountId) {
         if (!isAvailable()) {
             return KMSResponseStatus.NOT_AVAILABLE;
         }

         FbWallet fbWallet = getSecretStore(passphrase, accountId);
         if (fbWallet == null || AplCollectionUtils.isEmpty(fbWallet.getAllData())) {
             return KMSResponseStatus.BAD_CREDENTIALS;
         }
         List<Path> secretPaths = findKeyStorePaths(accountId);

         return deleteFileWithStatus(secretPaths);
     }

     public KMSResponseStatus deleteFileWithStatus(List<Path> paths) {
         try {
             for (Path path : paths) {
                 deleteFile(path);
             }
         } catch (IOException e) {
             LOG.debug("Unable to delete file. " + paths.get(0), e);
             return KMSResponseStatus.DELETE_ERROR;
         }
         return KMSResponseStatus.OK;
     }

     public void deleteFile(Path path) throws IOException {
         Files.delete(path);
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
         return keystoreDirPath.resolve(String.format(FORMAT, version, FORMATTER.format(utcTime), Convert2.defaultRsAccount(accountId)));
     }

     public boolean isNewVersionOfKeyStoreForAccountExist(long accountId) {
         Path path = findKeyStorePathWithLatestVersion(accountId);

         return path != null && isStorageVersionLatest(path);
     }

     public boolean isKeyStoreForAccountExist(long accountId) {
         return findKeyStorePathWithLatestVersion(accountId) != null;
     }

     private boolean isStorageVersionLatest(Path path) {
         return CURRENT_KEYSTORE_VERSION.equals(FbWalletUtil.getWalletFileVersion(path));
     }
 }
