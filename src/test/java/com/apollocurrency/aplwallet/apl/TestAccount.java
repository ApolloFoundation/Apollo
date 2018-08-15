/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;


import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestAccount {
    private long id;
    private byte[] publicKey;
    private String name;
    private String secretPhrase;
    private List<JSONTransaction> transactions = new ArrayList<>();

    public TestAccount(long id, byte[] publicKey, String name, String secretPhrase) {
        this.id = id;
        this.publicKey = publicKey;
        this.name = name;
        this.secretPhrase = secretPhrase;
    }
    public TestAccount(String secretPhrase) {
        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.id = Account.getId(publicKey);
        this.name = "";
        this.secretPhrase = secretPhrase;
    }


    public long getId() {
        return id;
    }

    public String getRS() {
        return Convert.rsAccount(getId());
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestAccount)) return false;
        TestAccount that = (TestAccount) o;
        return Objects.equals(secretPhrase, that.secretPhrase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secretPhrase);
    }
}
