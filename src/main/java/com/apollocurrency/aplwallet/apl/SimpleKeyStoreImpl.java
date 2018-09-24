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
 import java.time.OffsetDateTime;
 import java.time.ZoneOffset;
 import java.time.format.DateTimeFormatter;
 import java.util.List;
 import java.util.Objects;
 import java.util.stream.Collectors;

 public class SimpleKeyStoreImpl implements KeyStore {
     private Path keystoreDirPath;
     private byte version;
     private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
     private static final String format = "v%d_%s---%s";

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
         Path privateKeyPath = verify(findKeySeedPaths(accountId), accountId);
         byte[] jsonFileBytes = Files.readAllBytes(privateKeyPath);
         JSONObject jsonObject = parseJSON(jsonFileBytes);
         byte[] encryptedKeyBytes = readBytes(jsonObject, "encryptedKeySeed");
         byte[] nonce = readBytes(jsonObject, "nonce");
         byte[] key = Crypto.getKeySeed(passphrase, nonce);
         try {
             byte[] decryptedKeySeed = Crypto.aesDecrypt(encryptedKeyBytes, key);
             long actualAccId = Convert.getId(Crypto.getPublicKey(decryptedKeySeed));
             if (accountId != actualAccId) {
                 throw new SecurityException("Passphrase is incorrect");
             }
             return decryptedKeySeed;
         }
         catch (RuntimeException e) {
             throw new SecurityException("Passphrase is incorrect");
         }

     }

     private JSONObject parseJSON(byte[] jsonFileBytes) {
         JSONObject jsonObject;
         try {
             jsonObject = (JSONObject) new JSONParser().parse(new String(jsonFileBytes));
         }
         catch (ParseException e) {
             throw new RuntimeException("Unable to parse pk json", e);
         }
         return jsonObject;
     }

     private byte[] generateBytes(int size) {
         byte[] nonce = new byte[size];
         Crypto.getSecureRandom().nextBytes(nonce);
         return nonce;
     }

     protected byte[] readBytes(JSONObject jsonObject, String name) {
         String bytes = (String) jsonObject.get(name);
         if (bytes == null) {
             throw new RuntimeException("'" + name + " should not be null ");
         }
         return Convert.parseHexString(bytes);
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
         Objects.requireNonNull(passphrase);
         Objects.requireNonNull(keySeed);
         Instant instant = Instant.now();
         OffsetDateTime utcTime = instant.atOffset( ZoneOffset.UTC );
         long accountId = Convert.getId(Crypto.getPublicKey(keySeed));
         List<Path> keySeedPaths = findKeySeedPaths(accountId);
         if (keySeedPaths.size() != 0) {
             throw new RuntimeException("Unable to save keySeed");
         }
         Path keyPath = keystoreDirPath.resolve(String.format(format, version, formatter.format(utcTime), Convert.rsAccount(accountId)));
         byte[] nonce = generateBytes(16);
         byte[] key = Crypto.getKeySeed(passphrase, nonce);
         byte[] encryptedKeySeed = Crypto.aesEncrypt(keySeed, key);
         JSONObject keySeedJSON = getJSONObject(accountId, encryptedKeySeed, keySeed.length, nonce);
         storeJSONKeySeed(keyPath, keySeedJSON);
     }

     protected void storeJSONKeySeed(Path keyPath, JSONObject keySeedJSON) throws IOException {
         Files.write(keyPath, keySeedJSON.toJSONString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
     }

     protected JSONObject getJSONObject(long accountId, byte[] encryptedKeySeed, int keySeedSize, byte[] nonce) {
         JSONObject keyJson = new JSONObject();
         keyJson.put("encryptedKeySeed", Convert.toHexString(encryptedKeySeed));
         keyJson.put("accountRS", Convert.rsAccount(accountId));
         keyJson.put("account", accountId);
         keyJson.put("bytes", keySeedSize);
         keyJson.put("nonce", Convert.toHexString(nonce));
         return keyJson;
     }
 }
