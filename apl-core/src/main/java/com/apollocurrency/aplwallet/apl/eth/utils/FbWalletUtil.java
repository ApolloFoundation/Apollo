 /*
  * Copyright © 2019 Apollo Foundation
  */

 package com.apollocurrency.aplwallet.apl.eth.utils;

 import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.nio.file.Path;
 import java.util.regex.Pattern;

 public class FbWalletUtil {
     private static final Logger LOG = LoggerFactory.getLogger(FbWalletUtil.class);


     public static Integer getWalletFileVersion(Path path) {
         return getWalletFileVersion(path.toString());
     }

     public static Integer getWalletFileVersion(String path) {
         try {
             String[] folders = path.split(Pattern.quote(File.separator));
             String fileVersion = folders[folders.length - 1].split("_")[0].substring(1);
             return Integer.valueOf(fileVersion);
         } catch (Exception ex) {
             LOG.warn(ex.getMessage());
             return null;
         }
     }

     public static String getAccount(String path) {
         try {
             int beginIndex = path.indexOf("---");
             if (beginIndex == -1) {
                 return null;
             }

             return path.substring(beginIndex + 3);
         } catch (Exception ex) {
             LOG.warn(ex.getMessage());
             return null;
         }
     }

     public static ApolloFbWallet buildWallet(byte[] keyStore, String passPhrase) {
         if (passPhrase == null || keyStore == null || keyStore.length == 0) {
             return null;
         }

         try {
             ApolloFbWallet fbWallet = new ApolloFbWallet();
             fbWallet.readOpenData(new ByteArrayInputStream(keyStore));
             byte[] key = fbWallet.keyFromPassPhrase(passPhrase, fbWallet.getContanerIV());
             fbWallet.openStream(new ByteArrayInputStream(keyStore), key);
             return fbWallet;
         } catch (Exception ex) {
             LOG.error(ex.getMessage());
             return null;
         }
     }


 }
