/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Account2FA extends BasicAccount {
    @JsonAlias("errorDescription")
    private Status2FA status;

    @JsonCreator
    public Account2FA(@JsonProperty(value = "account", required = true) String account,
                      @JsonProperty("status") Status2FA status2FA) {
        super(account);
        this.status = status2FA;
    }

    public Status2FA getStatus2FA() {
        return status;
    }

    @Override
    public String toString() {
        return "Account2FA{" +
                "status=" + status +
                ", id=" + Crypto.rsEncode(getId()) +
                '}';
    }
}
