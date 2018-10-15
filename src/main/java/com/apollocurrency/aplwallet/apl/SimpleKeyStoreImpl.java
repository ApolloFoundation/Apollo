 /*
  * Copyright © 2018 Apollo Foundation
  */

 package com.apollocurrency.aplwallet.apl;

 import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
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

 public class SimpleKeyStoreImpl implements KeyStore {
         private static final Logger LOG = getLogger(SimpleKeyStoreImpl.class);
     private Path keystoreDirPath;
     private byte version;
     private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
     private static final String FORMAT = "v%d_%s---%s";

     public SimpleKeyStoreImpl(Path keyStoreDirPath, byte version) {
         if (version < 0) {
             throw new IllegalArgumentException("version should be positive");
         }
         this.version = version;
         this.keystoreDirPath = keyStoreDirPath;
         if (!Files.exists(keyStoreDirPath)) {
             try {
                 Files.createDirectories(keyStoreDirPath);
             }
             catch (IOException e) {
                 throw new RuntimeException(e.toString(), e);
             }
         }
     }

     @Override
     public SecretBytesDetails getSecretBytes(String passphrase, long accountId)  {
         Objects.requireNonNull(passphrase);

         List<Path> secretPaths = findSecretPaths(accountId);

         if (secretPaths.size() == 0) {
             return new SecretBytesDetails(null, SecretBytesDetails.ExtractStatus.NOT_FOUND);
         } else if (secretPaths.size() > 1) {
             return new SecretBytesDetails(null, SecretBytesDetails.ExtractStatus.DUPLICATE_FOUND);
         }

         Path privateKeyPath = secretPaths.get(0);
         try {
             EncryptedSecretBytesDetails secretBytesDetails =
                     JSON.getMapper().readValue(privateKeyPath.toFile(), EncryptedSecretBytesDetails.class);

         byte[] key = Crypto.getKeySeed(passphrase, secretBytesDetails.getNonce(), Convert.longToBytes(secretBytesDetails.getTimestamp()));
             byte[] decryptedSecretBytes = Crypto.aesDecrypt(secretBytesDetails.getEncryptedSecretBytes(), key);

             long actualAccId = Convert.getId(Crypto.getPublicKey(Crypto.getKeySeed(decryptedSecretBytes)));
             if (accountId != actualAccId) {
                 return new SecretBytesDetails(null, SecretBytesDetails.ExtractStatus.BAD_CREDENTIALS);
             }
             return new SecretBytesDetails(decryptedSecretBytes, SecretBytesDetails.ExtractStatus.OK);

         }
         catch (IOException e) {
             return new SecretBytesDetails(null, SecretBytesDetails.ExtractStatus.READ_ERROR);
         }
         catch (RuntimeException e) {
             return new SecretBytesDetails(null, SecretBytesDetails.ExtractStatus.DECRYPTION_ERROR);
         }
     }

     private byte[] generateBytes(int size) {
         byte[] nonce = new byte[size];
         Crypto.getSecureRandom().nextBytes(nonce);
         return nonce;
     }


     protected List<Path> findSecretPaths(long accountId) {
         try {
             return
                     Files.list(keystoreDirPath).filter(path -> {
                         String stringPath = path.toString();
                         int beginIndex = stringPath.indexOf("---");
                         if (beginIndex == -1) {
                             return false;
                         }
                         return stringPath.substring(beginIndex + 3).equalsIgnoreCase(String.valueOf(Convert.rsAccount(accountId)));
                     }).collect(Collectors.toList());
         }
         catch (IOException e) {
             LOG.debug("KeyStore exception: {}", e.getMessage());
             return null;
         }
     }

     @Override
     public boolean saveSecretBytes(String passphrase, byte[] secretBytes) {
         Objects.requireNonNull(passphrase);
         Objects.requireNonNull(secretBytes);

         long accountId = Convert.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretBytes)));
         Path keyPath = makeTargetPath(accountId);
         if (keyPath == null) {
             return false;
         }
         EncryptedSecretBytesDetails secretBytesDetails = makeEncryptedSecretBytesDetails(passphrase, secretBytes, accountId);
         return storeJSONSecretBytes(keyPath, secretBytesDetails);
     }

     private EncryptedSecretBytesDetails makeEncryptedSecretBytesDetails(String passphrase, byte[] secretBytes, long accountId) {
         byte[] nonce = generateBytes(16);
         long timestamp = System.currentTimeMillis();
         byte[] key = Crypto.getKeySeed(passphrase, nonce, Convert.longToBytes(timestamp));
         byte[] encryptedSecretBytes = Crypto.aesEncrypt(secretBytes, key);
         return new EncryptedSecretBytesDetails(encryptedSecretBytes, accountId, secretBytes.length, version, nonce, timestamp);
     }

     private Path makeTargetPath(long accountId) {
         boolean isNew = isNewAccount(accountId);
         if (!isNew) {
             LOG.debug("Account already exist");
             return null;
         }
         Instant instant = Instant.now();
         OffsetDateTime utcTime = instant.atOffset( ZoneOffset.UTC );
         return keystoreDirPath.resolve(String.format(FORMAT, version, FORMATTER.format(utcTime), Convert.rsAccount(accountId)));
     }

     private boolean isNewAccount(long accountId) {
         List<Path> secretBytesPaths = findSecretPaths(accountId);
         return secretBytesPaths != null && secretBytesPaths.size() == 0;
     }

     protected boolean storeJSONSecretBytes(Path keyPath, EncryptedSecretBytesDetails secretBytesDetails) {
         try {
             ObjectMapper mapper = JSON.getMapper();
             ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
             Files.createFile(keyPath);
             writer.writeValue(keyPath.toFile(), secretBytesDetails);
             return true;
         }
         catch (IOException e) {
             LOG.debug("Unable to save secretBytes: {}", e.getMessage());
             return false;
         }
     }
 }
