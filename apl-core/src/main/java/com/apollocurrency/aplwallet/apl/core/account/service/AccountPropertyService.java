/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountPropertyService {

    void setProperty(AccountEntity account, Transaction transaction, AccountEntity setterAccount, String property, String value);

    void deleteProperty(AccountEntity account, long propertyId);
}
