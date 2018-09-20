 /*
  * Copyright © 2018 Apollo Foundation
  */

 package com.apollocurrency.aplwallet.apl;

 import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

 public class SimpleKeyStoreImpl implements KeyStore {
     private Path keystoreDirPath;


     public SimpleKeyStoreImpl(Path keyStoreDirPath) {
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
         Path privateKeyPath = verify(findKeySeedPaths(accountId), accountId);
         byte[] jsonFileBytes = Files.readAllBytes(privateKeyPath);

         byte[] encryptedKeyBytes = readEncryptedKeySeed(jsonFileBytes);

         byte[] key = Crypto.sha256().digest(Convert.toBytes(passphrase));
         byte[] decryptedKeySeed = Crypto.aesDecrypt(encryptedKeyBytes, key);
         long actualAccId = Convert.getId(Crypto.getPublicKey(decryptedKeySeed));
         if (accountId != actualAccId) {
             throw new SecurityException("Passphrase is incorrect");
         }
         return decryptedKeySeed;
     }

     protected byte[] readEncryptedKeySeed(byte[] bytes) {
         JSONObject jsonObject;
         try {
             jsonObject = (JSONObject) new JSONParser().parse(new String(bytes));
         }
         catch (ParseException e) {
             throw new RuntimeException("Unable to parse pk json", e);
         }
         String encryptedKey = (String) jsonObject.get("encryptedKeySeed");
         if (encryptedKey == null) {
             throw new RuntimeException("'encryptedKey' should not be null ");
         }
         return Convert.parseHexString(encryptedKey);
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

     protected Path verify(List<Path> keySeedFiles, long accountId) {
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
         Instant instant = Instant.now();
         long accountId = Convert.getId(Crypto.getPublicKey(keySeed));
         List<Path> keySeedPaths = findKeySeedPaths(accountId);
         if (keySeedPaths.size() != 0) {
             throw new RuntimeException("Unable to save keySeed");
         }
         Path keyPath = keystoreDirPath.resolve(instant.toString() + "---" + Convert.rsAccount(accountId));
         JSONObject jsonContent = new JSONObject();
         byte[] key = Crypto.sha256().digest(Convert.toBytes(passphrase));

         jsonContent.put("encryptedKeySeed", Convert.toHexString(Crypto.aesEncrypt(keySeed, key)));
         jsonContent.put("accountRS", Convert.rsAccount(accountId));
         jsonContent.put("account", accountId);
         jsonContent.put("bytes", keySeed.length);
         Files.write(keyPath, jsonContent.toJSONString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
     }
 }
