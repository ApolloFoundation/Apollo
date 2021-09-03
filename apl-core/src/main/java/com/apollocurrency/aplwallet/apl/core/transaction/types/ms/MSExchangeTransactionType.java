/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;

public abstract class MSExchangeTransactionType extends MSTransactionType {

    public MSExchangeTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, CurrencyService currencyService) {
        super(blockchainConfig, accountService, currencyService);
    }

    @Override
    public final void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemExchangeAttachment attachment = (MonetarySystemExchangeAttachment) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        currencyService.validate(currency, transaction);
        if (!currencyService.isActive(currency)) {
            throw new AplException.NotCurrentlyValidException("Currency not active: " + attachment.getJSONObject());
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemExchangeAttachment attachment = (MonetarySystemExchangeAttachment) transaction.getAttachment();
        if (attachment.getRateATM() <= 0 || attachment.getUnits() == 0) {
            throw new AplException.NotValidException("Invalid exchange: " + attachment.getJSONObject());
        }
        if (!getBlockchainConfig().isTotalAmountOverflowTx(transaction.getId())) {
            long orderTotalATM = Convert2.safeMultiply(attachment.getRateATM(), attachment.getUnits(), transaction);
            long maxBalanceATM = getBlockchainConfig().getCurrentConfig().getMaxBalanceATM();
            if (orderTotalATM > maxBalanceATM) {
                throw new AplException.NotValidException("Currency order total in ATMs: " + orderTotalATM + " is higher than max allowed: "
                    + maxBalanceATM + ", currency=" + Long.toUnsignedString(attachment.getCurrencyId()) + ", quantity="
                    + attachment.getUnits() + ", price=" + attachment.getRateATM());
            }
        }
    }

    @Override
    public final boolean canHaveRecipient() {
        return false;
    }

}
