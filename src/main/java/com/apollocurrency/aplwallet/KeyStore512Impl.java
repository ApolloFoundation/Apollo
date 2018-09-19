 /*
  * Copyright © 2018 Apollo Foundation
  */

 package com.apollocurrency.aplwallet;

 import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.KeyStore;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

 public class KeyStore512Impl implements KeyStore {
     private Path keystoreDirPath;


     public KeyStore512Impl(Path keyStoreDirPath) {
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
     public byte[] getPrivateKey(String passphrase, long accountId) throws IOException {
         List<Path> privateKeysFiles =
                 Files.list(keystoreDirPath).filter(path -> {
                     String stringPath = path.toString();
                     int beginIndex = stringPath.indexOf("---");
                     if (beginIndex == -1) {
                         return false;
                     }
                     return stringPath.substring(beginIndex + 3).equalsIgnoreCase(String.valueOf(Convert.rsAccount(accountId)));
                 }).collect(Collectors.toList());
         if (privateKeysFiles.size() == 0) {
             throw new RuntimeException("No private key, associated with id = " + accountId);
         }
         if (privateKeysFiles.size() > 1) {
             throw new RuntimeException("Found " + privateKeysFiles.size() + " private keys associated with id ="
                     + accountId + " . Expected only 1.");
         }
         Path privateKeyPath = privateKeysFiles.get(0);
         byte[] bytes = Files.readAllBytes(privateKeyPath);

         JSONObject jsonObject = null;
         try {
             jsonObject = (JSONObject) new JSONParser().parse(new String(bytes));
         }
         catch (ParseException e) {
             e.printStackTrace();
         }
         String encryptedKey = (String) jsonObject.get("encryptedKey");
         byte[] encryptedKeyBytes = Convert.parseHexString(encryptedKey);
         byte[] key = Convert.toBytes(passphrase);
         byte[] trimmedKey = Arrays.copyOf(key, 32);
         byte[] decryptedKey = Crypto.aesDecrypt(encryptedKeyBytes, trimmedKey);
         long actualAccId = Account.getNewId(Crypto.getNewPublicKey(decryptedKey));
         if (accountId != actualAccId) {
             throw new SecurityException("Passphrase is incorrect");
         }
         return decryptedKey;
     }

     @Override
     public void savePrivateKey(String passphrase, byte[] privateKey) throws IOException {
         Instant instant = Instant.now();
         long accountId = Account.getNewId(Crypto.getNewPublicKey(privateKey));
         Path keyPath = keystoreDirPath.resolve(instant.toString() + "---" + Convert.rsAccount(accountId));
         JSONObject object = new JSONObject();
         byte[] key = Convert.toBytes(passphrase);
         byte[] trimmedKey = Arrays.copyOf(key, 32);
         object.put("encryptedKey", Convert.toHexString(Crypto.aesEncrypt(privateKey, trimmedKey)));
         object.put("accountRS", Convert.rsAccount(accountId));
         object.put("account", accountId);
         Files.write(keyPath, object.toJSONString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
     }
 }
