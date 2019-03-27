/*
* Copyright © 2019 Apollo Foundation
*/

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.json.simple.JSONObject;

public class WalletKeysInfo {

    private AplWalletKey aplWalletKey;
    private EthWalletKey ethWalletKey;
    private EthWalletKey paxWalletKey;
    private String passphrase;

    public WalletKeysInfo(ApolloFbWallet apolloWallet, String passphrase) {
        this.aplWalletKey = apolloWallet.getAplWalletKey();
        this.ethWalletKey = apolloWallet.getEthWalletKey();
        this.paxWalletKey = apolloWallet.getPaxWalletKey();
        this.passphrase = passphrase;
    }

    public AplWalletKey getAplWalletKey() {
        return aplWalletKey;
    }

    public void setAplWalletKey(AplWalletKey aplWalletKey) {
        this.aplWalletKey = aplWalletKey;
    }

    public EthWalletKey getEthWalletKey() {
        return ethWalletKey;
    }

    public EthWalletKey getPaxWalletKey() {
        return paxWalletKey;
    }

    public void setPaxWalletKey(EthWalletKey paxWalletKey) {
        this.paxWalletKey = paxWalletKey;
    }

    public void setEthWalletKey(EthWalletKey ethWalletKey) {
        this.ethWalletKey = ethWalletKey;
    }

    public String getEthAddress() {
        return ethWalletKey.getCredentials().getAddress();
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

    @Deprecated
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("apl", getAplWalletKey().toJSON());
        jsonObject.put("eth", getEthWalletKey().toJSON());
        jsonObject.put("pax", getPaxWalletKey().toJSON());

         //For backward compatibility.
        jsonObject.put("account", getAplId());
        jsonObject.put("accountRS", Convert2.rsAccount(getAplId()));
        jsonObject.put("publicKey", Convert.toHexString(getAplWalletKey().getPublicKey()));
        jsonObject.put("ethAddress", getEthAddress());

        if (!StringUtils.isBlank(passphrase)) {
            jsonObject.put("passphrase", passphrase);
        }
        return jsonObject;
    }

    public JSONObject toJSON_v2() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("apl", getAplWalletKey().toJSON());
        jsonObject.put("eth", getEthWalletKey().toJSON());
        jsonObject.put("pax", getPaxWalletKey().toJSON());

        if (!StringUtils.isBlank(passphrase)) {
            jsonObject.put("passphrase", passphrase);
        }
        return jsonObject;
    }


}
