/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.vault.model;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import io.firstbridge.cryptolib.container.DataRecord;
import io.firstbridge.cryptolib.container.FbWallet;
import io.firstbridge.cryptolib.container.KeyRecord;
import io.firstbridge.cryptolib.container.KeyTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ApolloFbWallet extends FbWallet {
    /**
     * For backward compatibility
     */
    @Deprecated
    public static final String APL_SECRET_KEY_ALIAS = "apl_secret";

    public static final String APL_PRIVATE_KEY_ALIAS = "apl_priv";
    public static final String ETH_PRIVATE_KEY_ALIAS = "eth_priv";
    /**
     * Use only ETH_PRIVATE_KEY_ALIAS, we shouldn't split eth and erc20 tokes.
     * Because ERC20 should connect to the main eth address
     */
    @Deprecated
    public static final String PAX_PRIVATE_KEY_ALIAS = "pax_priv";

    private static final Integer HEXADECIMAL = 16;


    public String getAplKeySecret() {
        String secret = this.getAllData().stream()
            .filter(dataRecord -> Objects.equals(dataRecord.alias, APL_SECRET_KEY_ALIAS))
            .map(dataRecord -> dataRecord.data)
            .findFirst().orElse(null);

        return secret;
    }

    public AplWalletKey getAplWalletKey() {
        String secret = getAplKeySecret();
        return new AplWalletKey(Convert.parseHexString(secret));
    }

    public List<EthWalletKey> getEthWalletKeys() {
        List<EthWalletKey> ethWalletKeys = new ArrayList<>();
        ethWalletKeys.add(getEthOrToken(ETH_PRIVATE_KEY_ALIAS));

        //For backward compatibility
        EthWalletKey pax = getEthOrToken(PAX_PRIVATE_KEY_ALIAS);
        if (pax != null) {
            ethWalletKeys.add(pax);
        }
        return ethWalletKeys;
    }

    private EthWalletKey getEthOrToken(String alias) {
        String secret = this.getAllData().stream()
            .filter(dataRecord -> Objects.equals(dataRecord.alias, alias))
            .map(dataRecord -> dataRecord.data)
            .findFirst().orElse(null);

        return secret != null ? new EthWalletKey(Convert.parseHexString(secret)) : null;
    }

    public void addAplKey(AplWalletKey aplWalletKey) {
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

    public void addEthKey(EthWalletKey ethWalletKey) {
        DataRecord dr = new DataRecord();
        dr.alias = ETH_PRIVATE_KEY_ALIAS;
        dr.data = ethWalletKey.getCredentials().getEcKeyPair().getPrivateKey().toString(HEXADECIMAL);
        dr.encoding = "HEX";
        KeyRecord kr = new KeyRecord();
        kr.alias = dr.alias;
        kr.keyType = KeyTypes.ETHEREUM;
        kr.publicKey = ethWalletKey.getCredentials().getEcKeyPair().getPublicKey().toString(HEXADECIMAL);
        this.addData(dr);
        this.addKey(kr);
    }

}
