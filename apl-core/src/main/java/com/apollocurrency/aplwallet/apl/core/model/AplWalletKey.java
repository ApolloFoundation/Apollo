/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.core.account.BasicAccount;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
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

    public AplWalletKey(long id, byte[] publicKey, byte[] privateKey, byte[] secretBytes) {
        this.id = id;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
//        this.passphrase = passphrase;
        this.secretBytes = secretBytes;
    }

    public void setSecretBytes(byte[] secretBytes) {
        this.secretBytes = secretBytes;
    }

    public byte[] getSecretBytes() {
        return secretBytes;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("account", id);
        jsonObject.put("accountRS", Convert2.rsAccount(id));
        if (publicKey != null) {
            jsonObject.put("publicKey", Convert.toHexString(publicKey));
        }
        if (passphrase != null) {
            jsonObject.put("passphrase", passphrase);
        }
        return jsonObject;
    }
}

