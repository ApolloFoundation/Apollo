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

public class FbWalletUtil {

    public static final String APL_ALIAS = "apl";
    public static final String ETH_ALIAS = "eth";

    public static void addAplKey(AplWalletKey aplWalletKey, FbWallet fbWallet){
        DataRecord dr = new DataRecord();
        dr.alias = APL_ALIAS;
        dr.data = Convert.toHexString(aplWalletKey.getPrivateKey());
        dr.encoding = "HEX";
        KeyRecord kr = new KeyRecord();
        kr.alias = dr.alias;
        kr.keyType = KeyTypes.APOLLO_OLD;
        kr.publicKey = Convert.toHexString(aplWalletKey.getPublicKey());
        fbWallet.addData(dr);
        fbWallet.addKey(kr);
    }

    public static void addEthKey(EthWalletKey ethWalletKey, FbWallet fbWallet){
        DataRecord dr = new DataRecord();
        dr.alias = ETH_ALIAS;
        dr.data = ethWalletKey.getCredentials().getEcKeyPair().getPrivateKey().toString(16);
        dr.encoding = "HEX";
        KeyRecord kr = new KeyRecord();
        kr.alias = dr.alias;
        kr.keyType = KeyTypes.ETHEREUM;
        kr.publicKey = ethWalletKey.getCredentials().getEcKeyPair().getPublicKey().toString(16);
        fbWallet.addData(dr);
        fbWallet.addKey(kr);
    }
}
