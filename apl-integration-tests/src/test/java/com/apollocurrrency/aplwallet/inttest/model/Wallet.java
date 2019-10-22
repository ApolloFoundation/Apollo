package com.apollocurrrency.aplwallet.inttest.model;

public class Wallet {
    private String user;
    private String pass;
    private String publicKey;
    private boolean vault;


    public Wallet() {
    }

    public Wallet(String user, String pass, String publicKey, boolean vault) {
        this.user = user;
        this.pass = pass;
        this.publicKey = publicKey;
        this.vault = vault;
    }
    public Wallet(String user, String pass) {
        this.user = user;
        this.pass = pass;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public boolean isVault() {
        return vault;
    }

    public void setVault(boolean vault) {
        this.vault = vault;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    @Override
    public String toString() {
        if(!vault) return "Standart Wallet";
        else return "Vault Wallet";

    }
}
