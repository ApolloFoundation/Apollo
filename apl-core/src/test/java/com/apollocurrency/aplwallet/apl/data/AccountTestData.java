/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class AccountTestData {

    public static final long CREATOR_ID = 1739068987193023818L;

    public final Account ACC_G = createAccount(1      ,1739068987193023818L,999990000000000L,999990000000000L,false,0,0,0,true);
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

    public final int ACC_BLOCKCHAIN_HEIGHT = ACC_14.getHeight();
    public final int ACC_BLOCKCHAIN_WRONG_HEIGHT = ACC_14.getHeight()+1;

    public  List<Account> ALL_ACCOUNTS = List.of(ACC_G, ACC_0, ACC_1, ACC_2, ACC_3, ACC_4, ACC_5, ACC_6, ACC_7, ACC_8, ACC_9, ACC_10, ACC_11, ACC_12, ACC_13, ACC_14 );
    public Account newAccount = new Account(999L, ACC_14.getHeight()+1);




    public final AccountAsset ACC_ASS_0 = createAsset(2, 100, 10, 8, 8, 42716, true);
    public final AccountAsset ACC_ASS_1 = createAsset(3, 110, 10, 2, 2, 42716 , true);
    public final AccountAsset ACC_ASS_2 = createAsset(4, 120, 20, 1, 1, 74579 , true);
    public final AccountAsset ACC_ASS_3 = createAsset(7, 130, 30, 10000000000000L, 10000000000000L, 103547, true);
    public final AccountAsset ACC_ASS_4 = createAsset(9, 140, 30, 200000000000000L, 199690000000000L, 104313, true);
    public final AccountAsset ACC_ASS_5 = createAsset(11, 150, 40, 100000000, 0, 106009, true);
    public final AccountAsset ACC_ASS_6 = createAsset(15, 160, 50, 1000000000, 1000000000, 115621, true);
    public final AccountAsset ACC_ASS_7 = createAsset(16, 170, 50, 1000000000, 1000000000, 115621, true);
    public final AccountAsset ACC_ASS_8 = createAsset(17, 180, 50, 1000000000, 1000000000, 115621, true);
    public final AccountAsset ACC_ASS_9 = createAsset(18, 190, 50, 997000000000L, 997000000000L, 115625, true);
    public final AccountAsset ACC_ASS_10= createAsset(21, 200, 60, 50000, 1000, 135786, true);
    public final AccountAsset ACC_ASS_11= createAsset(24, 210, 70, 1, 1, 141149, true);
    public final AccountAsset ACC_ASS_12= createAsset(26, 220, 80, 1, 1, 157464, true);
    public final AccountAsset ACC_ASS_13= createAsset(27, 220, 90, 1, 1, 161462, true);
    public final AccountAsset ACC_ASS_14= createAsset(28, 230, 100, 1, 1, 163942, true);

    public final int ASS_BLOCKCHAIN_HEIGHT = ACC_ASS_14.getHeight();
    public final int ASS_BLOCKCHAIN_WRONG_HEIGHT = ACC_ASS_14.getHeight()+1;

    public List<AccountAsset> ALL_ASSETS = List.of(ACC_ASS_0, ACC_ASS_1, ACC_ASS_2, ACC_ASS_3, ACC_ASS_4, ACC_ASS_5, ACC_ASS_6, ACC_ASS_7, ACC_ASS_8, ACC_ASS_9, ACC_ASS_10, ACC_ASS_11, ACC_ASS_12, ACC_ASS_13, ACC_ASS_14);
    public AccountAsset newAsset = new AccountAsset(ACC_1.getId(), ACC_ASS_14.getAssetId()+1, 0, 0, ACC_ASS_14.getHeight()+1);


    public final AccountCurrency ACC_CUR_0 = createCurrency(4, 100, 10, 2000000, 2000000, 9800, true);
    public final AccountCurrency ACC_CUR_1 = createCurrency(5, 110, 10, 9899999998000000L, 9899999998000000L, 23208, true);
    public final AccountCurrency ACC_CUR_2 = createCurrency(14, 120, 20, 100, 100, 99999, true);
    public final AccountCurrency ACC_CUR_3 = createCurrency(18, 130, 20, 100, 100, 100237, true);
    public final AccountCurrency ACC_CUR_4 = createCurrency(23, 140, 20, 100, 100, 101515, true);
    public final AccountCurrency ACC_CUR_5 = createCurrency(25, 150, 20, 9800, 9800, 101976, true);
    public final AccountCurrency ACC_CUR_6 = createCurrency(28, 160, 20, 10000, 10000, 103064, true);
    public final AccountCurrency ACC_CUR_7 = createCurrency(33, 120, 30, 25000, 25000, 104087, true);
    public final AccountCurrency ACC_CUR_8 = createCurrency(39, 170, 40, 10000000000L, 10000000000L, 107363, true);
    public final AccountCurrency ACC_CUR_9 = createCurrency(41, 180, 50, 10000000000L, 10000000000L, 107380, true);
    public final AccountCurrency ACC_CUR_10= createCurrency(42, 190, 60, 100000, 100000, 109087, true);
    public final AccountCurrency ACC_CUR_11= createCurrency(47, 200, 20, 19979000, 19979000, 114982, true);
    public final AccountCurrency ACC_CUR_12= createCurrency(48, 210, 20, 900, 900, 114982, true);
    public final AccountCurrency ACC_CUR_13= createCurrency(56, 220, 70, 2000000000, 2000000000, 124550, true);
    public final AccountCurrency ACC_CUR_14= createCurrency(57, 230, 80, 2000000000, 2000000000, 124607, true);

    public final int CUR_BLOCKCHAIN_HEIGHT = ACC_CUR_14.getHeight();
    public final int CUR_BLOCKCHAIN_WRONG_HEIGHT = ACC_CUR_14.getHeight()+1;

    public List<AccountCurrency> ALL_CURRENCY = List.of(ACC_CUR_0, ACC_CUR_1, ACC_CUR_2, ACC_CUR_3, ACC_CUR_4, ACC_CUR_5, ACC_CUR_6, ACC_CUR_7, ACC_CUR_8, ACC_CUR_9, ACC_CUR_10, ACC_CUR_11, ACC_CUR_12, ACC_CUR_13, ACC_CUR_14);
    public AccountCurrency newCurrency = new AccountCurrency(ACC_1.getId(), ACC_CUR_14.getCurrencyId()+1, 0, 0, ACC_CUR_14.getHeight()+1);




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

    public AccountAsset createAsset(long dbId, long accountId, long assetId, long quantity, long unconfirmedQuantity, int height, boolean latest){
        AccountAsset asset = new AccountAsset(accountId, assetId, quantity, unconfirmedQuantity, height);
        asset.setDbId(dbId);
        asset.setLatest(latest);
        return asset;
    }

    public AccountCurrency createCurrency(long dbId, long accountId, long currencyId, long quantity, long unconfirmedQuantity, int height, boolean latest){
        AccountCurrency currency = new AccountCurrency(accountId, currencyId, quantity, unconfirmedQuantity, height);
        currency.setDbId(dbId);
        currency.setLatest(latest);
        return currency;
    }



}
