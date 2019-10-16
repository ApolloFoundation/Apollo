/*
 * Copyright (c) 2018-2019. Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author al
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class BasicAccount extends BaseDTO {
    @JsonAlias({"account"}) // from json
    @JsonProperty("account") //to json
    private long id;
    public String accountRS;

    public BasicAccount(String account) {
        this.id = Convert.parseAccountId(account);
    }
    
}
