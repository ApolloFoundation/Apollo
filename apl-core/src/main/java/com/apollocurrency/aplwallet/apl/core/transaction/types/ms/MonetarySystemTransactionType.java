/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemAttachment;

import java.util.Map;

public abstract class MonetarySystemTransactionType extends TransactionType {

    public MonetarySystemTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        MonetarySystemAttachment attachment = (MonetarySystemAttachment) transaction.getAttachment();
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        String nameLower = currency.getName().toLowerCase();
        String codeLower = currency.getCode().toLowerCase();
        boolean isDuplicate = TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.CURRENCY_ISSUANCE, nameLower, duplicates, false);
        if (!nameLower.equals(codeLower)) {
            isDuplicate = isDuplicate || TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.CURRENCY_ISSUANCE, codeLower, duplicates, false);
        }
        return isDuplicate;
    }

    @Override
    public final boolean isPhasingSafe() {
        return false;
    }


}
