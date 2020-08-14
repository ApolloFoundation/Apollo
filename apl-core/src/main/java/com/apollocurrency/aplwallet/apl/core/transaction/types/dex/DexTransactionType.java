/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.dex;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;

public abstract class DexTransactionType extends TransactionType {

    protected final DexService dexService;


    public DexTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, DexService dexService) {
        super(blockchainConfig, accountService);
        this.dexService = dexService;
    }
}
