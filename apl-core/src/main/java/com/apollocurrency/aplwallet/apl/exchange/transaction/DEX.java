/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.transaction;

import javax.enterprise.inject.spi.CDI;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.exchange.DexConfig;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;

public abstract class DEX extends TransactionType {

    private DexService dexService;
    private DexConfig dexConfig;

    @Override
    public byte getType() {
        return TransactionType.TYPE_DEX;
    }


    public static final TransactionType DEX_ORDER_TRANSACTION = new DexOrderTransaction();
    public static final TransactionType DEX_CANCEL_ORDER_TRANSACTION = new DexCancelOrderTransaction();
    public static final TransactionType DEX_CONTRACT_TRANSACTION = new DexContractTransaction();
    public static final TransactionType DEX_TRANSFER_MONEY_TRANSACTION = new DexTransferMoneyTransaction();
    public static final TransactionType DEX_CLOSE_ORDER = new DexCloseOrderTransaction();

    public DexService lookupDexService(){
        if ( dexService == null) {
            dexService = CDI.current().select(DexService.class).get();
        }
        return dexService;
    }

    public DexConfig lookupDexConfig(){
        if ( dexConfig == null) {
            dexConfig = CDI.current().select(DexConfig.class).get();
        }
        return dexConfig;
    }

}
