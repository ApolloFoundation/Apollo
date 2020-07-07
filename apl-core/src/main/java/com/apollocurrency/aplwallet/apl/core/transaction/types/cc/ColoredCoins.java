/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;

/**
 * @author al
 */
public abstract class ColoredCoins extends TransactionType {

    protected AccountAssetService accountAssetService;

    public ColoredCoins(BlockchainConfig blockchainConfig, AccountService accountService, AccountAssetService accountAssetService) {
        super(blockchainConfig, accountService);
        this.accountAssetService = accountAssetService;
    }
}
