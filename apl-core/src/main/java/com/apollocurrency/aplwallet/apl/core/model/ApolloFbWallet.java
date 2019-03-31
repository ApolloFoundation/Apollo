/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import io.firstbridge.cryptolib.container.DataRecord;
import io.firstbridge.cryptolib.container.FbWallet;
import io.firstbridge.cryptolib.container.KeyRecord;
import io.firstbridge.cryptolib.container.KeyTypes;

import java.util.Objects;

public class ApolloFbWallet extends FbWallet {
    /**
     * For backward compatibility
     */
    @Deprecated
    public static final String APL_SECRET_KEY_ALIAS = "apl_secret";

    public static final String APL_PRIVATE_KEY_ALIAS = "apl_priv";
    public static final String ETH_PRIVATE_KEY_ALIAS = "eth_priv";
    public static final String PAX_PRIVATE_KEY_ALIAS = "pax_priv";



    public String getAplKeySecret(){
        String secret = this.getAllData().stream()
                .filter(dataRecord -> Objects.equals(dataRecord.alias, APL_SECRET_KEY_ALIAS))
                .map(dataRecord -> dataRecord.data)
                .findFirst().orElse(null);

        return secret;
    }

    public AplWalletKey getAplWalletKey(){
        String secret = getAplKeySecret();
        return new AplWalletKey(Convert.parseHexString(secret));
    }

    public EthWalletKey getEthWalletKey(){
        return getEthOrToken(ETH_PRIVATE_KEY_ALIAS);
    }

    public EthWalletKey getPaxWalletKey(){
        return getEthOrToken(PAX_PRIVATE_KEY_ALIAS);
    }

    private EthWalletKey getEthOrToken(String alias){
        String secret = this.getAllData().stream()
                .filter(dataRecord -> Objects.equals(dataRecord.alias, alias))
                .map(dataRecord -> dataRecord.data)
                .findFirst().orElse(null);

        EthWalletKey ethWalletKey = new EthWalletKey(Convert.parseHexString(secret));

        return ethWalletKey;
    }

    public void addAplKey(AplWalletKey aplWalletKey){
        DataRecord dr = new DataRecord();
        dr.alias = APL_PRIVATE_KEY_ALIAS;
        dr.data = Convert.toHexString(aplWalletKey.getPrivateKey());
        dr.encoding = "HEX";
        KeyRecord kr = new KeyRecord();
        kr.alias = dr.alias;
        kr.keyType = KeyTypes.APOLLO_OLD;
        kr.publicKey = Convert.toHexString(aplWalletKey.getPublicKey());
        this.addData(dr);
        this.addKey(kr);

        DataRecord drSecretK = new DataRecord();
        drSecretK.alias = APL_SECRET_KEY_ALIAS;
        drSecretK.data = Convert.toHexString(aplWalletKey.getSecretBytes());
        drSecretK.encoding = "HEX";
        KeyRecord krSecretK = new KeyRecord();
        krSecretK.alias = drSecretK.alias;
        krSecretK.keyType = KeyTypes.APOLLO_OLD;
        krSecretK.publicKey = Convert.toHexString(aplWalletKey.getPublicKey());
        this.addData(drSecretK);
        this.addKey(krSecretK);
    }

    public void addEthKey(EthWalletKey ethWalletKey){
        createEthOrEthTokenWallet(ethWalletKey, ETH_PRIVATE_KEY_ALIAS);
    }

    public void addPaxKey(EthWalletKey ethWalletKey){
       createEthOrEthTokenWallet(ethWalletKey, PAX_PRIVATE_KEY_ALIAS);
    }

    private void createEthOrEthTokenWallet(EthWalletKey ethWalletKey, String alias){
        DataRecord dr = new DataRecord();
        dr.alias = alias;
        dr.data = ethWalletKey.getCredentials().getEcKeyPair().getPrivateKey().toString(16);
        dr.encoding = "HEX";
        KeyRecord kr = new KeyRecord();
        kr.alias = dr.alias;
        kr.keyType = KeyTypes.ETHEREUM;
        kr.publicKey = ethWalletKey.getCredentials().getEcKeyPair().getPublicKey().toString(16);
        this.addData(dr);
        this.addKey(kr);
    }



}
