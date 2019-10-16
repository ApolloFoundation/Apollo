package com.apollocurrrency.aplwallet.inttest.model;

public class Wallet {
    private String user;
    private String pass;
    private String publicKey;
    private String secretKey;


    public Wallet() {
    }

    public Wallet(String user, String pass, String publicKey, String secretKey) {
        this.user = user;
        this.pass = pass;
        this.publicKey = publicKey;
        this.secretKey = secretKey;
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

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    @Override
    public String toString() {
        if(secretKey == null) return "Standart Wallet";
        else return "Vault Wallet";

    }
}
