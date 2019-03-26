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

    public WalletKeysInfo(ApolloFbWallet apolloWallet) {
        this.aplWalletKey = apolloWallet.getAplWalletKey();
        this.ethWalletKey = apolloWallet.getEthWalletKey();
    }

    public WalletKeysInfo(AplWalletKey aplWalletKey, EthWalletKey ethWalletKey) {
        this.aplWalletKey = aplWalletKey;
        this.ethWalletKey = ethWalletKey;
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
        return aplWalletKey.getPassphrase();
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("apl", getAplWalletKey().toJSON());
        jsonObject.put("eth", getEthWalletKey().toJSON());

         //For backward compatibility.
        jsonObject.put("account", getAplId());
        jsonObject.put("accountRS", Convert2.rsAccount(getAplId()));
        jsonObject.put("publicKey", Convert.toHexString(getAplWalletKey().getPublicKey()));
        jsonObject.put("ethAddress", getEthAddress());

        if (!StringUtils.isBlank(getAplWalletKey().getPassphrase())) {
            jsonObject.put("passphrase", getAplWalletKey().getPassphrase());
        }
        return jsonObject;
    }


}
