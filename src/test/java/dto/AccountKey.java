/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.util.Convert;

public class AccountKey {
    private String account;
    private byte[] keySeed;

    public AccountKey(String account, byte[] keySeed) {
        this.account = account;
        this.keySeed = keySeed;
    }

    public AccountKey() {
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public byte[] getKeySeed() {
        return keySeed;
    }

    public void setKeySeed(byte[] keySeed) {
        this.keySeed = keySeed;
    }
    public void setKeySeed(String keySeed) {
        this.keySeed = Convert.parseHexString(keySeed);
    }
}
