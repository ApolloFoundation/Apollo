/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountProperty;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountPropertyService {

    AccountProperty getProperty(long propertyId);

    AccountProperty getProperty(long recipientId, String property, long setterId);

    List<AccountProperty> getProperties(long recipientId, long setterId, String property, int from, int to);

    void setProperty(Account account, Transaction transaction, Account setterAccount, String property, String value);

    void deleteProperty(Account account, long propertyId);
}
