/*
 * Copyright (c) 2018-2019. Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author al
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BasicAccount extends BaseDTO {
    public String accountRS;
    @JsonAlias({"account"}) // from json
    @JsonProperty("account") //to json
    protected long id;

    BasicAccount(long decodedAccount) {
        this.id = decodedAccount;
    }

}
