/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.AccountPropertyTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.Setter;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountPropertyServiceImpl implements AccountPropertyService {

    @Inject @Setter
    private AccountPropertyTable accountPropertyTable;

    @Inject @Setter
    private Event<Account> accountEvent;

    @Inject @Setter
    private Event<AccountProperty> accountPropertyEvent;

    @Override
    public void setProperty(Account account, Transaction transaction, Account setterAccount, String property, String value) {
        value = Convert.emptyToNull(value);
        AccountProperty accountProperty = AccountPropertyTable.getProperty(account.getId(), property, setterAccount.getId());
        if (accountProperty == null) {
            accountProperty = new AccountProperty(transaction.getId(), account.getId(), setterAccount.getId(), property, value);
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
        AccountProperty accountProperty = AccountPropertyTable.getInstance().get(AccountPropertyTable.newKey(propertyId));
        if (accountProperty == null) {
            return;
        }
        if (accountProperty.getSetterId() != account.getId() && accountProperty.getRecipientId() != account.getId()) {
            throw new RuntimeException("Property " + Long.toUnsignedString(propertyId) + " cannot be deleted by " + Long.toUnsignedString(account.getId()));
        }
        AccountPropertyTable.getInstance().delete(accountProperty);
        //accountService.listeners.notify(account, AccountEventType.DELETE_PROPERTY);
        accountEvent.select(literal(AccountEventType.DELETE_PROPERTY)).fire(account);
        //propertyListeners.notify(accountProperty, AccountEventType.DELETE_PROPERTY);
        accountPropertyEvent.select(literal(AccountEventType.DELETE_PROPERTY)).fire(accountProperty);
    }

}
