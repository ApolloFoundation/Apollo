package com.apollocurrency.aplwallet.apl.eth.model;

import lombok.EqualsAndHashCode;
import org.json.simple.JSONObject;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;

@EqualsAndHashCode
public class EthWalletKey {
    private Credentials credentials;

    public EthWalletKey(Credentials credentials) {
        this.credentials = credentials;
    }

    public EthWalletKey(byte[] privateKey) {
        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        this.credentials = Credentials.create(ecKeyPair);
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("address", credentials.getAddress());
        jsonObject.put("publicKey", credentials.getEcKeyPair().getPublicKey().toString(16));

        return jsonObject;
    }
}
