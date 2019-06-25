/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEvent;
import com.apollocurrency.aplwallet.apl.core.account.AccountPropertyTable;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountPropertyServiceImpl implements AccountPropertyService {

    @Inject @Setter
    private AccountPropertyTable accountPropertyTable;

    @Inject @Setter
    private AccountService accountService;

    @Override
    public void setProperty(AccountEntity accountEntity, Transaction transaction, AccountEntity setterAccount, String property, String value) {
        value = Convert.emptyToNull(value);
        AccountProperty accountProperty = AccountPropertyTable.getProperty(accountEntity.getId(), property, setterAccount.getId());
        if (accountProperty == null) {
            accountProperty = new AccountProperty(transaction.getId(), accountEntity.getId(), setterAccount.getId(), property, value);
        } else {
            accountProperty.setValue(value);
        }
        accountPropertyTable.insert(accountProperty);
        accountService.listeners.notify(accountEntity, AccountEvent.SET_PROPERTY);
        propertyListeners.notify(accountProperty, AccountEvent.SET_PROPERTY);

    }

    @Override
    public  void deleteProperty(AccountEntity accountEntity, long propertyId) {
        AccountProperty accountProperty = AccountPropertyTable.getInstance().get(AccountPropertyTable.newKey(propertyId));
        if (accountProperty == null) {
            return;
        }
        if (accountProperty.getSetterId() != accountEntity.getId() && accountProperty.getRecipientId() != accountEntity.getId()) {
            throw new RuntimeException("Property " + Long.toUnsignedString(propertyId) + " cannot be deleted by " + Long.toUnsignedString(accountEntity.getId()));
        }
        AccountPropertyTable.getInstance().delete(accountProperty);
        accountService.listeners.notify(accountEntity, AccountEvent.DELETE_PROPERTY);
        propertyListeners.notify(accountProperty, AccountEvent.DELETE_PROPERTY);
    }

}
