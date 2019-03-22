 /*
  * Copyright © 2019 Apollo Foundation
  */

package com.apollocurrency.aplwallet.apl.eth.utils;

 import com.apollocurrency.aplwallet.apl.core.model.AplWalletKey;
 import com.apollocurrency.aplwallet.apl.crypto.Convert;
 import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
 import io.firstbridge.cryptolib.container.DataRecord;
 import io.firstbridge.cryptolib.container.FbWallet;
 import io.firstbridge.cryptolib.container.KeyRecord;
 import io.firstbridge.cryptolib.container.KeyTypes;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.nio.file.Path;
 import java.util.Objects;
 import java.util.regex.Pattern;

 public class FbWalletUtil {
     private static final Logger LOG = LoggerFactory.getLogger(FbWalletUtil.class);

    public static final String APL_PRIVATE_KEY_ALIAS = "apl_priv";
     /**
      * For backward compatibility
      */
    @Deprecated
    public static final String APL_SECRET_KEY_ALIAS = "apl_secret";
    public static final String ETH_PRIVATE_KEY_ALIAS = "eth_priv";

    public static String getAplKeySecret(FbWallet fbWallet){
        if(fbWallet == null){
            return null;
        }

        String secret = fbWallet.getAllData().stream()
                .filter(dataRecord -> Objects.equals(dataRecord.alias, APL_SECRET_KEY_ALIAS))
                .map(dataRecord -> dataRecord.data)
                .findFirst().orElse(null);

        return secret;
    }

     public static EthWalletKey getEthPrivateKey(FbWallet fbWallet){
         if(fbWallet == null){
             return null;
         }

         String secret = fbWallet.getAllData().stream()
                 .filter(dataRecord -> Objects.equals(dataRecord.alias, ETH_PRIVATE_KEY_ALIAS))
                 .map(dataRecord -> dataRecord.data)
                 .findFirst().orElse(null);

         EthWalletKey ethWalletKey = new EthWalletKey(Convert.parseHexString(secret));

         return ethWalletKey;
     }

    public static void addAplKey(AplWalletKey aplWalletKey, FbWallet fbWallet){
        DataRecord dr = new DataRecord();
        dr.alias = APL_PRIVATE_KEY_ALIAS;
        dr.data = Convert.toHexString(aplWalletKey.getPrivateKey());
        dr.encoding = "HEX";
        KeyRecord kr = new KeyRecord();
        kr.alias = dr.alias;
        kr.keyType = KeyTypes.APOLLO_OLD;
        kr.publicKey = Convert.toHexString(aplWalletKey.getPublicKey());
        fbWallet.addData(dr);
        fbWallet.addKey(kr);

        DataRecord drSecretK = new DataRecord();
        drSecretK.alias = APL_SECRET_KEY_ALIAS;
        drSecretK.data = Convert.toHexString(aplWalletKey.getSecretBytes());
        drSecretK.encoding = "HEX";
        KeyRecord krSecretK = new KeyRecord();
        krSecretK.alias = drSecretK.alias;
        krSecretK.keyType = KeyTypes.APOLLO_OLD;
        krSecretK.publicKey = Convert.toHexString(aplWalletKey.getPublicKey());
        fbWallet.addData(drSecretK);
        fbWallet.addKey(krSecretK);
    }

    public static void addEthKey(EthWalletKey ethWalletKey, FbWallet fbWallet){
        DataRecord dr = new DataRecord();
        dr.alias = ETH_PRIVATE_KEY_ALIAS;
        dr.data = ethWalletKey.getCredentials().getEcKeyPair().getPrivateKey().toString(16);
        dr.encoding = "HEX";
        KeyRecord kr = new KeyRecord();
        kr.alias = dr.alias;
        kr.keyType = KeyTypes.ETHEREUM;
        kr.publicKey = ethWalletKey.getCredentials().getEcKeyPair().getPublicKey().toString(16);
        fbWallet.addData(dr);
        fbWallet.addKey(kr);
    }

     public static Integer getWalletFileVersion(Path path){
        return getWalletFileVersion(path.toString());
     }

     public static Integer getWalletFileVersion(String path){
        try {
            String[] folders = path.split(Pattern.quote(File.separator));
            String fileVersion = folders[folders.length - 1].split("_")[0].substring(1);
            return Integer.valueOf(fileVersion);
        } catch (Exception ex){
            LOG.warn(ex.getMessage(), ex);
            return null;
        }
     }

     public static String getAccount(String path){
         try {
             int beginIndex = path.indexOf("---");
             if (beginIndex == -1) {
                 return null;
             }

             return path.substring(beginIndex + 3);
         } catch (Exception ex){
             LOG.warn(ex.getMessage(), ex);
             return null;
         }
     }


     public static FbWallet buildWallet(byte[] keyStore, String passPhrase){
        if(passPhrase == null || keyStore == null || keyStore.length==0){
            return null;
        }

        try {
            FbWallet fbWallet = new FbWallet();
            fbWallet.readOpenData(new ByteArrayInputStream(keyStore));
            byte[] key = fbWallet.keyFromPassPhrase(passPhrase, fbWallet.getContanerIV());
            fbWallet.openStream(new ByteArrayInputStream(keyStore), key);
            return fbWallet;
        } catch (Exception ex){
            LOG.error(ex.getMessage(), ex);
            return null;
        }
     }


}
