/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.core.model.account.BasicAccount;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.json.simple.JSONObject;

import java.util.Objects;


public class AplWalletKey extends BasicAccount {
    private byte[] publicKey;
    @JsonIgnore
    private byte[] privateKey;
    private String passphrase;
    @JsonIgnore
    private byte[] secretBytes;

    public AplWalletKey(long id, byte[] publicKey, byte[] privateKey, byte[] secretBytes) {
        this.id = id;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.secretBytes = secretBytes;
    }

    public AplWalletKey(byte[] secretBytes) {
        byte[] keySeed = Crypto.getKeySeed(secretBytes);
        byte[] privateKey = Crypto.getPrivateKey(keySeed);
        byte[] accountPublicKey = Crypto.getPublicKey((keySeed));
        long accountId = Convert.getId(accountPublicKey);

        this.id = accountId;
        this.publicKey = accountPublicKey;
        this.privateKey = privateKey;
        this.secretBytes = secretBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AplWalletKey)) return false;
        AplWalletKey that = (AplWalletKey) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = Convert.parseHexString(publicKey);
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = Convert.parseHexString(privateKey);
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public byte[] getSecretBytes() {
        return secretBytes;
    }

    public void setSecretBytes(byte[] secretBytes) {
        this.secretBytes = secretBytes;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("currency", "apl");


        JSONObject innerJsonObject = new JSONObject();

        innerJsonObject.put("account", id);
        innerJsonObject.put("accountRS", Convert2.rsAccount(id));
        if (publicKey != null) {
            innerJsonObject.put("publicKey", Convert.toHexString(publicKey));
        }
        if (passphrase != null) {
            innerJsonObject.put("passphrase", passphrase);
        }

        jsonObject.put("wallets", innerJsonObject);
        return jsonObject;
    }
}

