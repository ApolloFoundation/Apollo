/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class AccountTestData {

    public final Account ACC_G = createAccount(1      ,1739068987193023818L,1999999000000L,1999999000000L,false,0,0,0,true);
    public final Account ACC_0 = createAccount(10     ,50L        ,555500000000L       ,105500000000L          ,false ,0               ,0 ,100000 ,true);
    public final Account ACC_1 = createAccount(20     ,100L       ,100000000L          ,100000000L             ,false ,0               ,0 ,104595 ,true );
    public final Account ACC_2 = createAccount(30     ,200L       ,250000000L          ,200000000L             ,false ,0               ,0 ,104670 ,true );
    public final Account ACC_3 = createAccount(40     ,7821792282123976600L       ,15025000000000L     ,14725000000000L        ,false ,0               ,0 ,105000 ,true );
    public final Account ACC_4 = createAccount(50     ,9211698109297098287L       ,25100000000000L     ,22700000000000L        ,false ,0               ,0 ,106000 ,true );
    public final Account ACC_5 = createAccount(60     ,500L       ,77182383705332315L  ,77182383705332315L     ,false ,0               ,0 ,141839 ,false);
    public final Account ACC_6 = createAccount(70     ,500L       ,77216366305332315L  ,77216366305332315L     ,false ,0               ,0 ,141844 ,false);
    public final Account ACC_7 = createAccount(80     ,500L       ,77798522705332315L  ,77798522705332315L     ,false ,0               ,0 ,141853 ,true);
    public final Account ACC_8 = createAccount(90     ,600L       ,40767800000000L     ,40767800000000L        ,false ,0               ,0 ,141855 ,false);
    public final Account ACC_9 = createAccount(100    ,600L       ,41167700000000L     ,41167700000000L        ,false ,0               ,0 ,141858 ,true);
    public final Account ACC_10= createAccount(110    ,700L       ,2424711969422000L   ,2424711969422000L      ,false ,1150030000000L  ,0 ,141860 ,true);
    public final Account ACC_11= createAccount(120    ,800L       ,2424711869422000L   ,2424711869422000L      ,false ,1150030000000L  ,0 ,141862 ,false);
    public final Account ACC_12= createAccount(130    ,800L       ,2424711769422000L   ,2424711769422000L      ,false ,1150030000000L  ,0 ,141864 ,false);
    public final Account ACC_13= createAccount(140    ,800L       ,77200915499807515L  ,77200915499807515L     ,false ,0               ,0 ,141866 ,false);
    public final Account ACC_14= createAccount(150    ,800L       ,40367900000000L     ,40367900000000L        ,false ,0               ,0 ,141868 ,false);

    public final int BLOCKCHAIN_HEIGHT = ACC_14.getHeight();
    public final int BLOCKCHAIN_WRONG_HEIGHT = ACC_14.getHeight()+1;

    public  List<Account> ALL_ACCOUNTS = List.of(ACC_G, ACC_0, ACC_1, ACC_2, ACC_3, ACC_4, ACC_5, ACC_6, ACC_7, ACC_8, ACC_9, ACC_10, ACC_11, ACC_12, ACC_13, ACC_14 );

    public Account newAccount = new Account(999L, ACC_14.getHeight()+1);

    public Account createAccount(long dbId, long accountId, long balance, long unconfirmedBalance, boolean isControlPhasing, long forgedBalance, long activeLessId, int height, boolean latest){
        Account acc = new Account(accountId, balance, unconfirmedBalance, forgedBalance, activeLessId, height);
        if (isControlPhasing) {
            acc.setControls(Collections.unmodifiableSet(EnumSet.of(AccountControlType.PHASING_ONLY)));
        }
        acc.setDbId(dbId);
        //acc.setDbKey(AccountTable.newKey(accountId));
        acc.setLatest(latest);
        return acc;
    }



}
