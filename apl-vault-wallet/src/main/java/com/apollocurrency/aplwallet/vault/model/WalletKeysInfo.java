/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.vault.model;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WalletKeysInfo {

    private AplWalletKey aplWalletKey;
    private List<EthWalletKey> ethWalletKeys = new ArrayList<>();
    private String passphrase;

    public WalletKeysInfo(ApolloFbWallet apolloWallet, String passphrase) {
        this.aplWalletKey = apolloWallet.getAplWalletKey();
        ethWalletKeys.addAll(apolloWallet.getEthWalletKeys());
        this.passphrase = passphrase;
    }

    public AplWalletKey getAplWalletKey() {
        return aplWalletKey;
    }

    public void setAplWalletKey(AplWalletKey aplWalletKey) {
        this.aplWalletKey = aplWalletKey;
    }

    public List<EthWalletKey> getEthWalletKeys() {
        return ethWalletKeys;
    }

    public Long getAplId() {
        return aplWalletKey.getId();
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public EthWalletKey getEthWalletForAddress(String address) {
        for (EthWalletKey ethWalletKey : ethWalletKeys) {
            if (ethWalletKey.getCredentials().getAddress().equals(address)) {
                return ethWalletKey;
            }
        }
        return null;
    }


    @Deprecated
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("apl", getAplWalletKey().toJSON());

        List<JSONObject> ethWallets = new ArrayList<>();
        for (EthWalletKey ethWallet : getEthWalletKeys()) {
            ethWallets.add(ethWallet.toJSON());
        }

        jsonObject.put("eth", ethWallets);

        //For backward compatibility.
        jsonObject.put("account", getAplId());
        jsonObject.put("accountRS", Convert2.rsAccount(getAplId()));
        jsonObject.put("publicKey", Convert.toHexString(getAplWalletKey().getPublicKey()));

        if (!StringUtils.isBlank(passphrase)) {
            jsonObject.put("passphrase", passphrase);
        }
        return jsonObject;
    }

    public JSONObject toJSON_v2() {
        JSONObject jsonObject = new JSONObject();
        List<JSONObject> currencies = new ArrayList<>();

        JSONObject ethObject = new JSONObject();
        List<JSONObject> ethWallets = new ArrayList<>();
        for (EthWalletKey ethWallet : getEthWalletKeys()) {
            ethWallets.add(ethWallet.toJSON());
        }

        ethObject.put("currency", "eth");
        ethObject.put("wallets", ethWallets);

        currencies.add(ethObject);

        jsonObject.put("currencies", currencies);

        if (!StringUtils.isBlank(passphrase)) {
            jsonObject.put("passphrase", passphrase);
        }

        return jsonObject;
    }


}
