package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingParams;

public class AccountControlPhasingData {
    //DB_ID, ACCOUNT_ID,          WHITELIST,          VOTING_MODEL, QUORUM, MIN_BALANCE, HOLDING_ID, MIN_BALANCE_MODEL, MAX_FEES, MIN_DURATION, MAX_DURATION, HEIGHT, LATEST
    // 10,   7995581942006468815, null,                 0,          1,        null,       null,          0,            300000000,    12,         113,         500,     true
    // 20, 2728325718715804811, (-8446656647637444484), 0,          1,        null,       null,          0,            300000000,    12,         113,         1000,    true
    // 30, -8446384352342482748,
    //      (2728325718715804811, 1344527020205736624), 0,          1,        null,       null,          0,             300000000,    12,        113,          2000,    true
    // 40, -4013722529644937202,
    // (-8446656647637444484, 1344527020205736624, -6724281675870110558), 0, 1, null,     null,          0,             300000000,    12,         113,          3000,   true
    public final AccountControlPhasing AC_CONT_PHAS_0 = new AccountControlPhasing(new LongKey(10), 7995581942006468815L,
        new PhasingParams((byte)0, 0L, 1L, 0L, (byte)0, new long[]{}),
        300000000, (short)12, (short)113, 500);
    public final AccountControlPhasing AC_CONT_PHAS_1 = new AccountControlPhasing(new LongKey(20), 2728325718715804811L,
        new PhasingParams((byte)0, 0L, 1L, 0L, (byte)0, new long[]{-8446656647637444484L}),
        300000000, (short)12, (short)113, 1000);
    public final AccountControlPhasing AC_CONT_PHAS_2 = new AccountControlPhasing(new LongKey(30), -8446384352342482748L,
        new PhasingParams((byte)0, 0L, 1L, 0L, (byte)0, new long[]{2728325718715804811L, 1344527020205736624L}),
        300000000, (short)12, (short)113, 2000);
    public final AccountControlPhasing AC_CONT_PHAS_3 = new AccountControlPhasing(new LongKey(40), 2728325718715804811L,
        new PhasingParams((byte)0, 0L, 1L, 0L, (byte)0, new long[]{-8446656647637444484L, 1344527020205736624L, -6724281675870110558L}),
        300000000, (short)12, (short)113, 3000);

    public final AccountControlPhasing NEW_AC_CONT_PHAS = new AccountControlPhasing(2728325718715804811L,
        new PhasingParams((byte)0, 0L, 1L, 1L, (byte)0, new long[]{-6724281675870110558L}),
        300000000, (short)12, (short)113, 4000);
}
