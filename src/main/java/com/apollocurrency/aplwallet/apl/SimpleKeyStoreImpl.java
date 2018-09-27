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
     private final SecurityException INCORRECT_PASSPHRASE_EXCEPTION = new SecurityException("Passphrase is incorrect");
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

         Path privateKeyPath = verifyExistOnlyOne(findKeySeedPaths(accountId), accountId);
         EncryptedKeySeedDetails keySeedDetails = JSON.getMapper().readValue(privateKeyPath.toFile(), EncryptedKeySeedDetails.class);

         byte[] key = Crypto.getKeySeed(passphrase, keySeedDetails.getNonce(), Convert.longToBytes(keySeedDetails.getTimestamp()));
         try {
             byte[] decryptedKeySeed = Crypto.aesDecrypt(keySeedDetails.getEncryptedKeySeed(), key);

             long actualAccId = Convert.getId(Crypto.getPublicKey(decryptedKeySeed));
             if (accountId != actualAccId) {
                 throw INCORRECT_PASSPHRASE_EXCEPTION;
             }
             return decryptedKeySeed;
         }
         catch (RuntimeException e) {
             throw INCORRECT_PASSPHRASE_EXCEPTION;
         }

     }

     private byte[] generateBytes(int size) {
         byte[] nonce = new byte[size];
         Crypto.getSecureRandom().nextBytes(nonce);
         return nonce;
     }


     protected List<Path> findKeySeedPaths(long accountId) throws IOException {
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

     protected Path verifyExistOnlyOne(List<Path> keySeedFiles, long accountId) {
         if (keySeedFiles.size() == 0) {
             throw new RuntimeException("No private key, associated with id = " + accountId);
         }
         if (keySeedFiles.size() > 1) {
             throw new RuntimeException("Found " + keySeedFiles.size() + " private keys associated with id ="
                     + accountId + " . Expected only 1.");
         }
         return keySeedFiles.get(0);
     }

     @Override
     public void saveKeySeed(String passphrase, byte[] keySeed) throws IOException {
         Objects.requireNonNull(passphrase);
         Objects.requireNonNull(keySeed);

         long accountId = Convert.getId(Crypto.getPublicKey(keySeed));
         Path keyPath = makeTargetPath(accountId);
         EncryptedKeySeedDetails keySeedDetails = makeEncryptedKeySeedDetails(passphrase, keySeed, accountId);
         storeJSONKeySeed(keyPath, keySeedDetails);
     }

     private EncryptedKeySeedDetails makeEncryptedKeySeedDetails(String passphrase, byte[] keySeed, long accountId) {
         byte[] nonce = generateBytes(16);
         long timestamp = System.currentTimeMillis();
         byte[] key = Crypto.getKeySeed(passphrase, nonce, Convert.longToBytes(timestamp));
         byte[] encryptedKeySeed = Crypto.aesEncrypt(keySeed, key);
         return new EncryptedKeySeedDetails(encryptedKeySeed, accountId, keySeed.length, version, nonce, timestamp);
     }

     private Path makeTargetPath(long accountId) throws IOException {
         requireNewAccount(accountId);
         Instant instant = Instant.now();
         OffsetDateTime utcTime = instant.atOffset( ZoneOffset.UTC );
         return keystoreDirPath.resolve(String.format(FORMAT, version, FORMATTER.format(utcTime), Convert.rsAccount(accountId)));
     }

     private void requireNewAccount(long accountId) throws IOException {
         List<Path> keySeedPaths = findKeySeedPaths(accountId);
         if (keySeedPaths.size() != 0) {
             throw new RuntimeException("Unable to save keySeed");
         }
     }

     protected void storeJSONKeySeed(Path keyPath, EncryptedKeySeedDetails keySeedDetails) throws IOException {
         ObjectMapper mapper = JSON.getMapper();
         ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
         Files.createFile(keyPath);
         writer.writeValue(keyPath.toFile(), keySeedDetails);
     }
 }
