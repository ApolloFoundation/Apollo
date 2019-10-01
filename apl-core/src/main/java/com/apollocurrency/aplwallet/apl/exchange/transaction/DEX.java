/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;

public abstract class DEX extends TransactionType {

    @Override
    public byte getType() {
        return TransactionType.TYPE_DEX;
    }


    public static final TransactionType DEX_ORDER_TRANSACTION = new DexOrderTransaction();
    public static final TransactionType DEX_CANCEL_ORDER_TRANSACTION = new DexCancelOrderTransaction();
    public static final TransactionType DEX_CONTRACT_TRANSACTION = new DexContractTransaction();
    public static final TransactionType DEX_TRANSFER_MONEY_TRANSACTION = new DexTransferMoneyTransaction();
    public static final TransactionType DEX_CLOSE_ORDER = new DexCloseOrderTransaction();

}
