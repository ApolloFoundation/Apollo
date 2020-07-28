/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyMintService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.MonetaryCurrencyMintingService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class MSCurrencyMintingTransactionType extends MonetarySystemTransactionType {
    private final CurrencyMintService currencyMintService;
    private final MonetaryCurrencyMintingService monetaryCurrencyMintingService;


    @Inject
    public MSCurrencyMintingTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, CurrencyService currencyService, CurrencyMintService currencyMintService, MonetaryCurrencyMintingService monetaryCurrencyMintingService) {
        super(blockchainConfig, accountService, currencyService);
        this.currencyMintService = currencyMintService;
        this.monetaryCurrencyMintingService = monetaryCurrencyMintingService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_CURRENCY_MINTING;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CURRENCY_MINTING;
    }

    @Override
    public String getName() {
        return "CurrencyMinting";
    }

    @Override
    public MonetarySystemCurrencyMinting parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemCurrencyMinting(buffer);
    }

    @Override
    public MonetarySystemCurrencyMinting parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemCurrencyMinting(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemCurrencyMinting attachment = (MonetarySystemCurrencyMinting) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        CurrencyType.validate(currency, transaction);
        if (attachment.getUnits() <= 0) {
            throw new AplException.NotValidException("Invalid number of units: " + attachment.getUnits());
        }
        if (attachment.getUnits() > (currency.getMaxSupply() - currency.getReserveSupply()) / Constants.MAX_MINTING_RATIO) {
            throw new AplException.NotValidException(String.format("Cannot mint more than 1/%d of the total units supply in a single request", Constants.MAX_MINTING_RATIO));
        }
        if (!currencyService.isActive(currency)) {
            throw new AplException.NotCurrentlyValidException("Currency not currently active " + attachment.getJSONObject());
        }
        long counter = currencyMintService.getCounter(attachment.getCurrencyId(), transaction.getSenderId());
        if (attachment.getCounter() <= counter) {
            throw new AplException.NotCurrentlyValidException(String.format("Counter %d has to be bigger than %d", attachment.getCounter(), counter));
        }
        if (!monetaryCurrencyMintingService.meetsTarget(transaction.getSenderId(), currency, attachment)) {
            throw new AplException.NotCurrentlyValidException(String.format("Hash doesn't meet target %s", attachment.getJSONObject()));
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemCurrencyMinting attachment = (MonetarySystemCurrencyMinting) transaction.getAttachment();
        currencyMintService.mintCurrency(getLedgerEvent(), transaction.getId(), senderAccount, attachment);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        MonetarySystemCurrencyMinting attachment = (MonetarySystemCurrencyMinting) transaction.getAttachment();
        return TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.MS_CURRENCY_MINTING, attachment.getCurrencyId() + ":" + transaction.getSenderId(), duplicates, true) || super.isDuplicate(transaction, duplicates);
    }

    @Override
    public boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        MonetarySystemCurrencyMinting attachment = (MonetarySystemCurrencyMinting) transaction.getAttachment();
        return TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.MS_CURRENCY_MINTING, attachment.getCurrencyId() + ":" + transaction.getSenderId(), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

}
