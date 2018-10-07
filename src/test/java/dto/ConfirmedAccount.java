/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.BasicAccount;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ConfirmedAccount extends BasicAccount {
    private boolean confirmed;

    public ConfirmedAccount(boolean confirmed) {
        this.confirmed = confirmed;
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
        if (!(o instanceof ConfirmedAccount)) return false;
        if (!super.equals(o)) return false;
        ConfirmedAccount that = (ConfirmedAccount) o;
        return confirmed == that.confirmed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), confirmed);
    }
    @JsonCreator
    public ConfirmedAccount(@JsonProperty(value = "account", required = true) String account, @JsonProperty("confirmed") boolean confirmed) {
        super(account);
        this.confirmed = confirmed;
    }

    @Override
    public String toString() {
        return "ConfirmedAccount{" +
                "confirmed=" + confirmed +
                ", id=" + getAccountRS() +
                '}';
    }
}
