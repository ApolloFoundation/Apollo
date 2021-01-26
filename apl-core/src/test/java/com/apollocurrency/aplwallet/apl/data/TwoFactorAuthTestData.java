/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.vault.model.TwoFactorAuthEntity;
import org.apache.commons.codec.binary.Base32;

public class TwoFactorAuthTestData extends AccountTestData {
    public static final int MAX_2FA_ATTEMPTS = 2;
    public static final String ACCOUNT2_2FA_SECRET_HEX = "f3e0475e0db85a822037";
    public static final byte[] ACCOUNT2_2FA_SECRET_BYTES = Convert.parseHexString(ACCOUNT2_2FA_SECRET_HEX);
    public static final String ACCOUNT2_2FA_SECRET_BASE32 = new Base32().encodeToString(ACCOUNT2_2FA_SECRET_BYTES);
    public static final String ACCOUNT3_2FA_SECRET_HEX = "0b2ad0bba220ed225868";
    public static final byte[] ACCOUNT3_2FA_SECRET_BYTES = Convert.parseHexString(ACCOUNT3_2FA_SECRET_HEX);
    public static final String ACCOUNT3_2FA_SECRET_BASE32 = new Base32().encodeToString(ACCOUNT3_2FA_SECRET_BYTES);
    public static final int INVALID_CODE = 100200;
    public static final String INVALID_PASSPHRASE = "InvalidPassphrase";
    public final String ACCOUNT1_2FA_SECRET_HEX = "a3f312570b65671a7101";
    public final byte[] ACCOUNT1_2FA_SECRET_BYTES = Convert.parseHexString(ACCOUNT1_2FA_SECRET_HEX);
    public final String ACCOUNT1_2FA_SECRET_BASE32 = new Base32().encodeToString(ACCOUNT1_2FA_SECRET_BYTES);
    public final TwoFactorAuthEntity ENTITY1 = new TwoFactorAuthEntity(ACC_1.getId(), ACCOUNT1_2FA_SECRET_BYTES, true);
    public final TwoFactorAuthEntity ENTITY2 = new TwoFactorAuthEntity(ACC_2.getId(), ACCOUNT2_2FA_SECRET_BYTES, false);
    public final TwoFactorAuthEntity NEW_ENTITY = new TwoFactorAuthEntity(newAccount.getId(), ACCOUNT3_2FA_SECRET_BYTES, false);

    public TwoFactorAuthTestData() {
    }

}
