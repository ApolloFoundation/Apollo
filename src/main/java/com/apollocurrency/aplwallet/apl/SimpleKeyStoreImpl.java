 /*
  * Copyright © 2018 Apollo Foundation
  */

 package com.apollocurrency.aplwallet.apl;

 import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

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
     private static final String BAD_CREDENTIALS = "Bad credentials";
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
     public byte[] getKeySeed(String passphrase, long accountId) throws IOException, SecurityException {
         Objects.requireNonNull(passphrase);

         Path privateKeyPath = verifyExistOnlyOne(findSecretPaths(accountId), accountId);
         EncryptedSecretBytesDetails secretBytesDetails = JSON.getMapper().readValue(privateKeyPath.toFile(), EncryptedSecretBytesDetails.class);

         byte[] key = Crypto.getKeySeed(passphrase, secretBytesDetails.getNonce(), Convert.longToBytes(secretBytesDetails.getTimestamp()));
         try {
             byte[] decryptedSecretBytes = Crypto.aesDecrypt(secretBytesDetails.getEncryptedSecretBytes(), key);

             long actualAccId = Convert.getId(Crypto.getPublicKey(Crypto.getKeySeed(decryptedSecretBytes)));
             if (accountId != actualAccId) {
                 throw new SecurityException(BAD_CREDENTIALS);
             }
             return decryptedSecretBytes;
         }
         catch (RuntimeException e) {
             throw new SecurityException(BAD_CREDENTIALS);
         }

     }

     private byte[] generateBytes(int size) {
         byte[] nonce = new byte[size];
         Crypto.getSecureRandom().nextBytes(nonce);
         return nonce;
     }


     protected List<Path> findSecretPaths(long accountId) throws IOException {
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

     protected Path verifyExistOnlyOne(List<Path> secretBytesFiles, long accountId) {
         if (secretBytesFiles.size() == 0) {
             throw new RuntimeException("No private key, associated with id = " + accountId);
         }
         if (secretBytesFiles.size() > 1) {
             throw new RuntimeException("Found " + secretBytesFiles.size() + " private keys associated with id ="
                     + accountId + " . Expected only 1.");
         }
         return secretBytesFiles.get(0);
     }

     @Override
     public void saveSecretBytes(String passphrase, byte[] secretBytes) throws IOException {
         Objects.requireNonNull(passphrase);
         Objects.requireNonNull(secretBytes);

         long accountId = Convert.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretBytes)));
         Path keyPath = makeTargetPath(accountId);
         EncryptedSecretBytesDetails secretBytesDetails = makeEncryptedSecretBytesDetails(passphrase, secretBytes, accountId);
         storeJSONSecretBytes(keyPath, secretBytesDetails);
     }

     private EncryptedSecretBytesDetails makeEncryptedSecretBytesDetails(String passphrase, byte[] secretBytes, long accountId) {
         byte[] nonce = generateBytes(16);
         long timestamp = System.currentTimeMillis();
         byte[] key = Crypto.getKeySeed(passphrase, nonce, Convert.longToBytes(timestamp));
         byte[] encryptedSecretBytes = Crypto.aesEncrypt(secretBytes, key);
         return new EncryptedSecretBytesDetails(encryptedSecretBytes, accountId, secretBytes.length, version, nonce, timestamp);
     }

     private Path makeTargetPath(long accountId) throws IOException {
         requireNewAccount(accountId);
         Instant instant = Instant.now();
         OffsetDateTime utcTime = instant.atOffset( ZoneOffset.UTC );
         return keystoreDirPath.resolve(String.format(FORMAT, version, FORMATTER.format(utcTime), Convert.rsAccount(accountId)));
     }

     private void requireNewAccount(long accountId) throws IOException {
         List<Path> secretBytesPaths = findSecretPaths(accountId);
         if (secretBytesPaths.size() != 0) {
             throw new RuntimeException("Unable to save secretBytes");
         }
     }

     protected void storeJSONSecretBytes(Path keyPath, EncryptedSecretBytesDetails secretBytesDetails) throws IOException {
         ObjectMapper mapper = JSON.getMapper();
         ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
         Files.createFile(keyPath);
         writer.writeValue(keyPath.toFile(), secretBytesDetails);
     }
 }
