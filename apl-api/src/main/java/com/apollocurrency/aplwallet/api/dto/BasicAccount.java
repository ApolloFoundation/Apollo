
package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author al
 */
public class BasicAccount {
    //@JsonAlias({"account", "accountRS"}) // from json
    @JsonProperty("account") //to json
    public long id;
    public String accountRS;
    @JsonAlias("secretPhrase")
    public String passphrase;

    public BasicAccount() {
    }
    
    public BasicAccount(long account) {
        this.id = account;
    }   
    public BasicAccount(String account) {
        this.id = Convert.parseAccountId(account);
    }
    
   }
