/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.util.Convert;

import java.util.Arrays;
import java.util.Objects;

public class EncryptedKeySeedDetails {
    private byte[] encryptedKeySeed;
    private String accountRS;
    private long account;
    private int size;
    private byte version;
    private byte[] nonce;
    private long timestamp;

    public EncryptedKeySeedDetails() {
    }

    public EncryptedKeySeedDetails(byte[] encryptedKeySeed, String accountRS, long account, int size, byte version, byte[] nonce, long timestamp) {
        this.encryptedKeySeed = encryptedKeySeed;
        this.accountRS = accountRS;
        this.account = account;
        this.size = size;
        this.version = version;
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    public EncryptedKeySeedDetails(byte[] encryptedKeySeed, long account, int size, byte version, byte[] nonce, long timestamp) {
        this.encryptedKeySeed = encryptedKeySeed;
        this.accountRS = Convert.rsAccount(account);
        this.account = account;
        this.size = size;
        this.version = version;
        this.nonce = nonce;
        this.timestamp = timestamp;
    }
    public EncryptedKeySeedDetails(byte[] encryptedKeySeed, String accountRS, int size, byte version, byte[] nonce, long timestamp) {
        this.encryptedKeySeed = encryptedKeySeed;
        this.accountRS = accountRS;
        this.account = Convert.parseAccountId(accountRS);
        this.size = size;
        this.version = version;
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    public byte[] getEncryptedKeySeed() {
        return encryptedKeySeed;
    }

    public void setEncryptedKeySeed(byte[] encryptedKeySeed) {
        this.encryptedKeySeed = encryptedKeySeed;
    }

    public String getAccountRS() {
        return accountRS;
    }

    public void setAccountRS(String accountRS) {
        this.accountRS = accountRS;
    }

    public long getAccount() {
        return account;
    }

    public void setAccount(long account) {
        this.account = account;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncryptedKeySeedDetails)) return false;
        EncryptedKeySeedDetails that = (EncryptedKeySeedDetails) o;
        return account == that.account &&
                size == that.size &&
                version == that.version &&
                timestamp == that.timestamp &&
                Arrays.equals(encryptedKeySeed, that.encryptedKeySeed) &&
                Objects.equals(accountRS, that.accountRS) &&
                Arrays.equals(nonce, that.nonce);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(accountRS, account, size, version, timestamp);
        result = 31 * result + Arrays.hashCode(encryptedKeySeed);
        result = 31 * result + Arrays.hashCode(nonce);
        return result;
    }
}
