 /*
  * Copyright © 2018 Apollo Foundation
  */

 package com.apollocurrency.aplwallet.apl.core.app;

 import static org.slf4j.LoggerFactory.getLogger;

 import com.apollocurrency.aplwallet.apl.crypto.Convert;
 import com.apollocurrency.aplwallet.apl.crypto.Crypto;
 import com.apollocurrency.aplwallet.apl.util.JSON;
 import com.apollocurrency.aplwallet.apl.util.NtpTime;
 import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.fasterxml.jackson.databind.ObjectWriter;
 import org.slf4j.Logger;

 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.time.Instant;
 import java.time.OffsetDateTime;
 import java.time.ZoneOffset;
 import java.time.format.DateTimeFormatter;
 import java.util.List;
 import java.util.Objects;
 import java.util.stream.Collectors;
 import javax.enterprise.inject.spi.CDI;
 import javax.inject.Inject;
 import javax.inject.Named;
 import javax.inject.Singleton;
@Singleton
 public class VaultKeyStoreImpl implements VaultKeyStore {
     private static final Logger LOG = getLogger(VaultKeyStoreImpl.class);
     private static final byte DEFAULT_VERSION = 0;
     private Path keystoreDirPath;
     private byte version;
     private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
     private static final String FORMAT = "v%d_%s---%s";
     private NtpTime ntpTime;

    @Inject
    public VaultKeyStoreImpl(@Named("keystoreDirPath")Path keystoreDir) {
        this(keystoreDir, DEFAULT_VERSION);
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

     @Override
     public SecretBytesDetails getSecretBytes(String passphrase, long accountId)  {
         Objects.requireNonNull(passphrase);

         List<Path> secretPaths = findSecretPaths(accountId);

         if (secretPaths.size() == 0) {
             return new SecretBytesDetails(null, Status.NOT_FOUND);
         } else if (secretPaths.size() > 1) {
             return new SecretBytesDetails(null, Status.DUPLICATE_FOUND);
         }

         Path privateKeyPath = secretPaths.get(0);
         try {
             EncryptedSecretBytesDetails secretBytesDetails =
                     JSON.getMapper().readValue(privateKeyPath.toFile(), EncryptedSecretBytesDetails.class);
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


     public List<Path> findSecretPaths(long accountId) {
         try {
             return
                     Files.list(keystoreDirPath).filter(path -> {
                         String stringPath = path.toString();
                         int beginIndex = stringPath.indexOf("---");
                         if (beginIndex == -1) {
                             return false;
                         }
                         return stringPath.substring(beginIndex + 3).equalsIgnoreCase(Convert.defaultRsAccount(accountId));
                     }).collect(Collectors.toList());
         }
         catch (IOException e) {
             LOG.debug("VaultKeyStore IO error while searching path to secret key of account " + accountId, e.getMessage());
             return null;
         }
     }

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

     @Override
     public Status deleteSecretBytes(String passphrase, long accountId) {
         if (!isAvailable()) {
             return Status.NOT_AVAILABLE;
         }
         SecretBytesDetails secretBytes = getSecretBytes(passphrase, accountId);
         if (secretBytes.getExtractStatus() != Status.OK) {
             return secretBytes.getExtractStatus();
         }
         Path secretBytesPath = findSecretPaths(accountId).get(0);

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
         boolean isNew = isNewAccount(accountId);
         if (!isNew) {
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

     private boolean isNewAccount(long accountId) {
         List<Path> secretBytesPaths = findSecretPaths(accountId);
         return secretBytesPaths != null && secretBytesPaths.size() == 0;
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
