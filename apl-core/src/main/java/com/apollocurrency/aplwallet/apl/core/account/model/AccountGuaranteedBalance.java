/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;

import java.util.Objects;

public class AccountGuaranteedBalance {
    private DbKey dbKey;
    private final long accountId;
    private final long additions;
    private final int height;

    public AccountGuaranteedBalance(long accountId, long additions, int height) {
        this.accountId = accountId;
        this.additions = additions;
        this.height = height;
    }

    public AccountGuaranteedBalance(DbKey dbKey, long accountId, long additions, int height) {
        this.dbKey = dbKey;
        this.accountId = accountId;
        this.additions = additions;
        this.height = height;
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getAdditions() {
        return additions;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountGuaranteedBalance)) return false;
        AccountGuaranteedBalance that = (AccountGuaranteedBalance) o;
        return accountId == that.accountId &&
                additions == that.additions &&
                height == that.height;
    }

    @Override
    public String toString() {
        return "AccountGuaranteedBalance{" +
                "  accountId=" + accountId +
                ", additions=" + additions +
                ", height=" + height +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, additions, height);
    }
}
