/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountPropertyTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountPropertyServiceImpl implements AccountPropertyService {

    private Blockchain blockchain;
    private AccountPropertyTable accountPropertyTable;
    private Event<Account> accountEvent;
    private Event<AccountProperty> accountPropertyEvent;

    @Inject
    public AccountPropertyServiceImpl(Blockchain blockchain, AccountPropertyTable accountPropertyTable, Event<Account> accountEvent, Event<AccountProperty> accountPropertyEvent) {
        this.blockchain = blockchain;
        this.accountPropertyTable = accountPropertyTable;
        this.accountEvent = accountEvent;
        this.accountPropertyEvent = accountPropertyEvent;
    }

    @Override
    public AccountProperty getProperty(long propertyId) {
        return accountPropertyTable.get(AccountPropertyTable.newKey(propertyId));
    }

    @Override
    public AccountProperty getProperty(long recipientId, String property, long setterId) {
        return accountPropertyTable.getProperty(recipientId, property, setterId);
    }

    @Override
    public List<AccountProperty> getProperties(long recipientId, long setterId, String property, int from, int to) {
        List<AccountProperty> result = new ArrayList<>();
        try(DbIterator<AccountProperty> leases = accountPropertyTable.getProperties(recipientId, setterId, property, from, to)) {
            leases.forEachRemaining(result::add);
        }
        return result;
    }

    @Override
    public void setProperty(Account account, Transaction transaction, Account setterAccount, String property, String value) {
        value = Convert.emptyToNull(value);
        AccountProperty accountProperty = accountPropertyTable.getProperty(account.getId(), property, setterAccount.getId());
        if (accountProperty == null) {
            accountProperty = new AccountProperty(transaction.getId(), account.getId(), setterAccount.getId(), property, value, blockchain.getHeight());
        } else {
            accountProperty.setValue(value);
        }
        accountPropertyTable.insert(accountProperty);
        //accountService.listeners.notify(account, AccountEventType.SET_PROPERTY);
        accountEvent.select(literal(AccountEventType.SET_PROPERTY)).fire(account);
        //propertyListeners.notify(accountProperty, AccountEventType.SET_PROPERTY);
        accountPropertyEvent.select(literal(AccountEventType.SET_PROPERTY)).fire(accountProperty);

    }

    @Override
    public  void deleteProperty(Account account, long propertyId) {
        AccountProperty accountProperty = accountPropertyTable.get(AccountPropertyTable.newKey(propertyId));
        if (accountProperty == null) {
            return;
        }
        if (accountProperty.getSetterId() != account.getId() && accountProperty.getRecipientId() != account.getId()) {
            throw new RuntimeException("Property " + Long.toUnsignedString(propertyId) + " cannot be deleted by " + Long.toUnsignedString(account.getId()));
        }
        accountPropertyTable.delete(accountProperty);
        //accountService.listeners.notify(account, AccountEventType.DELETE_PROPERTY);
        accountEvent.select(literal(AccountEventType.DELETE_PROPERTY)).fire(account);
        //propertyListeners.notify(accountProperty, AccountEventType.DELETE_PROPERTY);
        accountPropertyEvent.select(literal(AccountEventType.DELETE_PROPERTY)).fire(accountProperty);
    }

}
