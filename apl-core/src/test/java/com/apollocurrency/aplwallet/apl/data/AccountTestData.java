package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;

public class AccountTestData {
    public static final long ACCOUNT1_ID = 100L;
    public static final long ACCOUNT2_ID = 200L;
    public static final long ACCOUNT3_ID = 300L;
    public final AccountDTO ACCOUNT1 = new AccountDTO(ACCOUNT1_ID, Convert2.rsAccount(ACCOUNT1_ID),100_000_000L, 0, 100_000_000L );
    public final AccountDTO ACCOUNT2 = new AccountDTO(ACCOUNT2_ID, Convert2.rsAccount(ACCOUNT2_ID),250_000_000L, 0, 200_000_000L);
    public final AccountDTO ACCOUNT3 = new AccountDTO(ACCOUNT3_ID, Convert2.rsAccount(ACCOUNT3_ID),300_000_000L, 0, 1_000_000_000L);

}
