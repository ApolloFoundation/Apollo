/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Objects;

public class BasicAccount {
    @JsonAlias({"account", "accountRS"}) // from json
    @JsonProperty("account") //to json
    protected long id;

    public BasicAccount(long account) {
        this.id = account;
    }

    public BasicAccount(String account) {
        this.id = Convert.parseAccountId(account);
    }

    @Override
    public String toString() {
        return "BasicAccount{" +
                Convert2.rsAccount(id) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicAccount)) return false;
        BasicAccount that = (BasicAccount) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public long getId() {
        return id;
    }
    public String getAccountRS() {
        return Convert2.rsAccount(id);
    }

    @JsonSetter
    public void setId(String account) {
        this.id = Convert.parseAccountId(account);
    }

    public BasicAccount() {
    }
}
