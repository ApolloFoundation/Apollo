package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import java.util.stream.Stream;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.Exchange;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuance;

public interface CurrencyService {
    DbIterator<Currency> getAllCurrencies(int from, int to);

    int getCount();

    Currency getCurrency(long id);

    Currency getCurrencyByName(String name);

    Currency getCurrencyByCode(String code);

    /**
     * @deprecated
     */
    DbIterator<Currency> getCurrencyIssuedBy(long accountId, int from, int to);

    Stream<Currency> getCurrencyIssuedByAsStream(long accountId, int from, int to);

    /**
     * @deprecated
     */
    DbIterator<Currency> searchCurrencies(String query, int from, int to);

    Stream<Currency> searchCurrenciesStream(String query, int from, int to);

    /**
     * @deprecated
     */
    DbIterator<Currency> getIssuedCurrenciesByHeight(int height, int from, int to);

    Stream<Currency> getIssuedCurrenciesByHeightStream(int height, int from, int to);

    void addCurrency(LedgerEvent event, long eventId, Transaction transaction, Account senderAccount,
                     MonetarySystemCurrencyIssuance attachment);

    void increaseReserve(LedgerEvent event, long eventId, Account account, long currencyId, long amountPerUnitATM);

    void claimReserve(LedgerEvent event, long eventId, Account account, long currencyId, long units);

    void transferCurrency(LedgerEvent event, long eventId, Account senderAccount, Account recipientAccount,
                          long currencyId, long units);

    long getCurrentSupply(Currency currency);

    long getCurrentReservePerUnitATM(Currency currency);

    boolean isActive(Currency currency);

    CurrencySupply loadCurrencySupplyByCurrency(Currency currency);

    void increaseSupply(Currency currency, long units);

    /**
     * @deprecated
     */
    DbIterator<Exchange> getExchanges(long currencyId, int from, int to);

    /**
     * @deprecated
     */
    DbIterator<CurrencyTransfer> getTransfers(long currencyId, int from, int to);

    boolean canBeDeletedBy(Currency currency, long senderAccountId);

    void delete(Currency currency, LedgerEvent event, long eventId, Account senderAccount);

    void validate(Currency currency, Transaction transaction) throws AplException.ValidationException;

    void validate(int type, Transaction transaction) throws AplException.ValidationException;

    void validate(Currency currency, int type, Transaction transaction) throws AplException.ValidationException;

    void validateCurrencyNaming(long issuerAccountId, MonetarySystemCurrencyIssuance attachment) throws AplException.ValidationException;

}
