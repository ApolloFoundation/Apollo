/*
* Copyright © 2019 Apollo Foundation
*/

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import org.json.simple.JSONObject;

public class WalletsInfo {
    private String ethAddress;
    private Long aplId;
    private byte[] aplPublicKey;

    private String passphrase;

    public WalletsInfo(EthWalletKey ethWalletKey, AplWalletKey aplWalletKey, String passphrase) {
        this.ethAddress = ethWalletKey.getCredentials().getAddress();
        this.aplId = aplWalletKey.getId();
        this.aplPublicKey = aplWalletKey.getPublicKey();
        this.passphrase = passphrase;
    }

    public String getEthAddress() {
        return ethAddress;
    }

    public void setEthAddress(String ethAddress) {
        this.ethAddress = ethAddress;
    }

    public Long getAplId() {
        return aplId;
    }

    public void setAplId(Long aplId) {
        this.aplId = aplId;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("account", aplId);
        jsonObject.put("accountRS", Convert2.rsAccount(aplId));
        jsonObject.put("publicKey", Convert.toHexString(aplPublicKey));
        jsonObject.put("ethAddress", ethAddress);

        if (passphrase != null) {
            jsonObject.put("passphrase", passphrase);
        }
        return jsonObject;
    }


}
