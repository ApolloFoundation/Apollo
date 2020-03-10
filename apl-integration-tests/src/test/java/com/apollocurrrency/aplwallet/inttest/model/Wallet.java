package com.apollocurrrency.aplwallet.inttest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Wallet {
    private String user;
    private String pass;
    private String publicKey;
    private boolean vault;
    private String ethAddress;
    private String accountId;

    public Wallet(String user, String pass) {
        this.user = user;
        this.pass = pass;
    }

    public Wallet(String user, String pass, boolean vault) {
        this.user = user;
        this.pass = pass;
        this.vault = vault;
    }


    public Wallet(String user, String pass, boolean vault, String ethAddress, String accountId) {
        this.user = user;
        this.pass = pass;
        this.vault = vault;
        this.ethAddress = ethAddress;
        this.accountId = accountId;
    }

    @Override
    public String toString() {
        String walletType =  (vault)? "Vault Wallet: ":"Standard Wallet: ";
        return walletType + getUser();
    }
}
