/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;


import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import dto.JSONTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TestAccount extends BasicAccount {
    private byte[] publicKey;
    private String name;
    private String secretPhrase;
    private List<JSONTransaction> transactions = new ArrayList<>();

    public TestAccount(long id, byte[] publicKey, String name, String secretPhrase) {
        super(id);
        this.publicKey = publicKey;
        this.name = name;
        this.secretPhrase = secretPhrase;
    }
    public TestAccount(String secretPhrase) {
        this(Convert.getId(Crypto.getPublicKey(secretPhrase)), Crypto.getPublicKey(secretPhrase), null, secretPhrase);
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public String getName() {
        return name;
    }

    public String getSecretPhrase() {
        return secretPhrase;
    }

    public List<JSONTransaction> getTransactions() {
        return transactions;
    }

    @Override
    public String toString() {
        return "TestAccount{" +
                "publicKey=" + Arrays.toString(publicKey) +
                ", name='" + name + '\'' +
                ", secretPhrase='" + secretPhrase + '\'' +
                ", transactions=" + transactions +
                ", account=" + getAccountRS() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestAccount)) return false;
        if (!super.equals(o)) return false;
        TestAccount that = (TestAccount) o;
        return Arrays.equals(publicKey, that.publicKey) &&
                Objects.equals(name, that.name) &&
                Objects.equals(secretPhrase, that.secretPhrase) &&
                Objects.equals(transactions, that.transactions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), name, secretPhrase, transactions);
        result = 31 * result + Arrays.hashCode(publicKey);
        return result;
    }
}
