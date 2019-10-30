package com.apollocurrrency.aplwallet.inttest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Wallet {
    private String user;
    private String pass;
    private String publicKey;
    private boolean vault;

    public Wallet(String user, String pass) {
        this.user = user;
        this.pass = pass;
    }

    @Override
    public String toString() {
        if(!vault) return "Standart Wallet";
        else return "Vault Wallet";

    }
}
