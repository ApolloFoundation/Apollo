package com.apollocurrency.aplwallet.apl.core.model;

import org.json.simple.JSONObject;
import org.web3j.crypto.Credentials;

public class EthWalletKey {
    private Credentials credentials;
    private byte[] keySeed;

    public EthWalletKey(Credentials credentials, byte[] keySeed) {
        this.credentials = credentials;
        this.keySeed = keySeed;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public byte[] getKeySeed() {
        return keySeed;
    }

    public void setKeySeed(byte[] keySeed) {
        this.keySeed = keySeed;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("account Eth", credentials.getAddress());
        jsonObject.put("publicKey", credentials.getEcKeyPair().getPublicKey().toString(16));

        return jsonObject;
    }
}
