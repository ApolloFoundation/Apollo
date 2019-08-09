/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;

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




    public final AccountAsset ACC_ASS_0 = createAsset(2, -6165567093261530199L, -7893128314037493631L, 8, 8, 42716, true);
    public final AccountAsset ACC_ASS_1 = createAsset(3, 5321122420987797313L, -7893128314037493631L, 2, 2, 42716 , true);
    public final AccountAsset ACC_ASS_2 = createAsset(4, -7322647937425179011L, -6078517019219425255L, 1, 1, 74579 , true);
    public final AccountAsset ACC_ASS_3 = createAsset(7, -6778759751532475550L, -8295646607890337777L, 10000000000000L, 10000000000000L, 103547, true);
    public final AccountAsset ACC_ASS_4 = createAsset(9, 1999163479746643929L, -8295646607890337777L, 200000000000000L, 199690000000000L, 104313, true);
    public final AccountAsset ACC_ASS_5 = createAsset(11, 6869755601928778675L, -4052659063540799517L, 100000000, 0, 106009, true);
    public final AccountAsset ACC_ASS_6 = createAsset(15, -8961133557088364255L, 4606988662156325931L, 1000000000, 1000000000, 115621, true);
    public final AccountAsset ACC_ASS_7 = createAsset(16, -7982593098243895578L, 4606988662156325931L, 1000000000, 1000000000, 115621, true);
    public final AccountAsset ACC_ASS_8 = createAsset(17, -2978059282518897292L, 4606988662156325931L, 1000000000, 1000000000, 115621, true);
    public final AccountAsset ACC_ASS_9 = createAsset(18, -4482816538164919588L, 4606988662156325931L, 997000000000L, 997000000000L, 115625, true);
    public final AccountAsset ACC_ASS_10= createAsset(21, -6373532437740775524L, 5392821904843656674L, 50000, 1000, 135786, true);
    public final AccountAsset ACC_ASS_11= createAsset(24, -2019873104351139231L, 1816504597825817175L, 1, 1, 141149, true);
    public final AccountAsset ACC_ASS_12= createAsset(26, 4771674949294490876L, 1991918493240442696L, 1, 1, 157464, true);
    public final AccountAsset ACC_ASS_13= createAsset(27, 4771674949294490876L, 3138466618559186009L, 1, 1, 161462, true);
    public final AccountAsset ACC_ASS_14= createAsset(28, 8133755063160788231L, 8693638682715124444L, 1, 1, 163942, true);

    public final int ASS_BLOCKCHAIN_HEIGHT = ACC_ASS_14.getHeight();
    public final int ASS_BLOCKCHAIN_WRONG_HEIGHT = ACC_ASS_14.getHeight()+1;

    public List<AccountAsset> ALL_ASSETS = List.of(ACC_ASS_0, ACC_ASS_1, ACC_ASS_2, ACC_ASS_3, ACC_ASS_4, ACC_ASS_5, ACC_ASS_6, ACC_ASS_7, ACC_ASS_8, ACC_ASS_9, ACC_ASS_10, ACC_ASS_11, ACC_ASS_12, ACC_ASS_13, ACC_ASS_14);
    public AccountAsset newAsset = new AccountAsset(ACC_1.getId(), ACC_ASS_14.getAssetId()+1, 0, 0, ACC_ASS_14.getHeight()+1);






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



}
