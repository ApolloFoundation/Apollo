/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.util.Convert;
import dto.Account;
import org.apache.commons.codec.binary.Base32;

public class TwoFactorAuthTestData {
    public static final Account ACCOUNT1 = new Account(100_000_000L, 0, 100_000_000L, 100);
    public static final Account ACCOUNT2 = new Account(250_000_000L, 0, 200_000_000L, 200);
    public static final String ACCOUNT1_2FA_SECRET_HEX = "a3f312570b65671a7101";
    public static final byte[] ACCOUNT1_2FA_SECRET_BYTES = Convert.parseHexString(ACCOUNT1_2FA_SECRET_HEX);
    public static final String ACCOUNT1_2FA_SECRET_BASE32 = new Base32().encodeToString(ACCOUNT1_2FA_SECRET_BYTES);
    public static final int INVALID_CODE = 100200;
}
