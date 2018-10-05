/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.BasicAccount;
import com.apollocurrency.aplwallet.apl.util.Convert;

public class AccountWithKey extends BasicAccount {
    private byte[] keySeed;

    public AccountWithKey(String account, byte[] keySeed) {
        super(account);
        this.keySeed = keySeed;
    }
    public AccountWithKey(long account, byte[] keySeed) {
        super(account);
        this.keySeed = keySeed;
    }

    public AccountWithKey() {
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
