/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountPropertyService {

    void setProperty(Account account, Transaction transaction, Account setterAccount, String property, String value);

    void deleteProperty(Account account, long propertyId);
}
