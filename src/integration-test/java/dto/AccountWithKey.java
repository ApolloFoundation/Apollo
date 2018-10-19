/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.BasicAccount;
import com.apollocurrency.aplwallet.apl.util.Convert;

public class AccountWithKey extends BasicAccount {
    private byte[] secretBytes;

    public AccountWithKey(String account, byte[] secretBytes) {
        super(account);
        this.secretBytes = secretBytes;
    }
    public AccountWithKey(long account, byte[] secretBytes) {
        super(account);
        this.secretBytes = secretBytes;
    }

    public AccountWithKey() {
    }

    public byte[] getSecretBytes() {
        return secretBytes;
    }

    public void setSecretBytes(byte[] secretBytes) {
        this.secretBytes = secretBytes;
    }

    public void setKeySeed(String keySeed) {
        this.secretBytes = Convert.parseHexString(keySeed);
    }
}
