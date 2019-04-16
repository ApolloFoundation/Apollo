/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountGuaranteedBalance;

import java.util.Arrays;
import java.util.List;

public class AccountGuaranteedBalanceData {
    public final AccountGuaranteedBalance BALANCE_1 = new AccountGuaranteedBalance(457571885748888948L, 30000000000L, 1000);
    public final AccountGuaranteedBalance BALANCE_2 = new AccountGuaranteedBalance(457571885748888948L, 10000000000000L, 1500);
    public final AccountGuaranteedBalance BALANCE_3 = new AccountGuaranteedBalance(6110033502865709882L, 40000000000L, 2000);
    public final AccountGuaranteedBalance BALANCE_4 = new AccountGuaranteedBalance(457571885748888948L, 40000000000L, 2000);
    public final AccountGuaranteedBalance BALANCE_5 = new AccountGuaranteedBalance(457571885748888948L, 20000000000L, 15457);
    public final AccountGuaranteedBalance NEW_BALANCE = new AccountGuaranteedBalance(6110033502865709882L, 120000000000L, 15500);
    public final List<AccountGuaranteedBalance> BALANCES = Arrays.asList(BALANCE_5, BALANCE_4, BALANCE_3, BALANCE_2, BALANCE_1);
}
