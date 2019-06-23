package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountPropertyService {
    Listeners<AccountProperty, Account.Event> propertyListeners = new Listeners<>();

    static boolean addPropertyListener(Listener<AccountProperty> listener, Account.Event eventType) {
        return propertyListeners.addListener(listener, eventType);
    }

    static boolean removePropertyListener(Listener<AccountProperty> listener, Account.Event eventType) {
        return propertyListeners.removeListener(listener, eventType);
    }

    void setProperty(AccountEntity account, Transaction transaction, AccountEntity setterAccount, String property, String value);

    void deleteProperty(AccountEntity account, long propertyId);
}
