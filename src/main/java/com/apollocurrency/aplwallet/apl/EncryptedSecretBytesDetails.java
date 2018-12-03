/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.util.Arrays;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.util.Convert;

public class EncryptedSecretBytesDetails {
    private byte[] encryptedSecretBytes;
    private String accountRS;
    private long account;
    private byte version;
    private byte[] nonce;
    private long timestamp;

    public EncryptedSecretBytesDetails() {
    }

    public EncryptedSecretBytesDetails(byte[] encryptedSecretBytes, String accountRS, long account, byte version, byte[] nonce, long timestamp) {
        this.encryptedSecretBytes = encryptedSecretBytes;
        this.accountRS = accountRS;
        this.account = account;
        this.version = version;
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    public EncryptedSecretBytesDetails(byte[] encryptedSecretBytes, long account, byte version, byte[] nonce, long timestamp) {
        this(encryptedSecretBytes, Convert.defaultRsAccount(account), version, nonce, timestamp);
    }
    public EncryptedSecretBytesDetails(byte[] encryptedSecretBytes, String accountRS, byte version, byte[] nonce, long timestamp) {
        this(encryptedSecretBytes, accountRS, Convert.parseAccountId(accountRS), version, nonce, timestamp);
    }

    public byte[] getEncryptedSecretBytes() {
        return encryptedSecretBytes;
    }

    public void setEncryptedSecretBytes(byte[] encryptedSecretBytes) {
        this.encryptedSecretBytes = encryptedSecretBytes;
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
        if (!(o instanceof EncryptedSecretBytesDetails)) return false;
        EncryptedSecretBytesDetails that = (EncryptedSecretBytesDetails) o;
        return account == that.account &&
                version == that.version &&
                timestamp == that.timestamp &&
                Arrays.equals(encryptedSecretBytes, that.encryptedSecretBytes) &&
                Objects.equals(accountRS, that.accountRS) &&
                Arrays.equals(nonce, that.nonce);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(accountRS, account, version, timestamp);
        result = 31 * result + Arrays.hashCode(encryptedSecretBytes);
        result = 31 * result + Arrays.hashCode(nonce);
        return result;
    }
}
