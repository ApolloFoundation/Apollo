/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.db;

import java.util.Arrays;
import java.util.Objects;

public class TwoFactorAuthEntity implements Cloneable {
    private long account;
    private byte[] secret;
    private boolean confirmed;

    public TwoFactorAuthEntity(long account, byte[] secret, boolean confirmed) {
        this.account = account;
        this.secret = secret;
        this.confirmed = confirmed;
    }

    public TwoFactorAuthEntity() {
    }

    public long getAccount() {
        return account;
    }

    public void setAccount(long account) {
        this.account = account;
    }

    public byte[] getSecret() {
        return secret;
    }

    public void setSecret(byte[] secret) {
        this.secret = secret;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TwoFactorAuthEntity)) return false;
        TwoFactorAuthEntity that = (TwoFactorAuthEntity) o;
        return account == that.account &&
                confirmed == that.confirmed &&
                Arrays.equals(secret, that.secret);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(account, confirmed);
        result = 31 * result + Arrays.hashCode(secret);
        return result;
    }

    @Override
    public TwoFactorAuthEntity clone() throws CloneNotSupportedException {
        TwoFactorAuthEntity obj = (TwoFactorAuthEntity) super.clone();
        obj.setSecret(Arrays.copyOf(this.secret, this.secret.length));
        return obj;
    }
}
