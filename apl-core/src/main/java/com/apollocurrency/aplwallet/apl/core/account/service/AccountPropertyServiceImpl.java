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
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;
import static com.apollocurrency.aplwallet.apl.core.app.CollectionUtil.toList;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountPropertyServiceImpl implements AccountPropertyService {

    private final Blockchain blockchain;
    private final AccountPropertyTable accountPropertyTable;
    private final Event<Account> accountEvent;
    private final Event<AccountProperty> accountPropertyEvent;

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
        return toList( accountPropertyTable.getProperties(recipientId, setterId, property, from, to));
    }

    @Override
    public void setProperty(Account account, Transaction transaction, Account setterAccount, String property, String value) {
        value = Convert.emptyToNull(value);
        AccountProperty accountProperty = accountPropertyTable.getProperty(account.getId(), property, setterAccount.getId());
        if (accountProperty == null) {
            accountProperty = new AccountProperty(transaction.getId(), account.getId(), setterAccount.getId(), property, value, blockchain.getHeight());
        } else {
            accountProperty.setValue(value);
            accountProperty.setHeight(blockchain.getHeight());
        }
        accountPropertyTable.insert(accountProperty);
        accountEvent.select(literal(AccountEventType.SET_PROPERTY)).fire(account);
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
        accountProperty.setHeight(blockchain.getHeight());
        accountPropertyTable.delete(accountProperty);
        accountEvent.select(literal(AccountEventType.DELETE_PROPERTY)).fire(account);
        accountPropertyEvent.select(literal(AccountEventType.DELETE_PROPERTY)).fire(accountProperty);
    }

}
