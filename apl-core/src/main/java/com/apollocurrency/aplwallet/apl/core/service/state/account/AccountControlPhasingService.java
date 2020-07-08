/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SetPhasingOnly;
import com.apollocurrency.aplwallet.apl.core.app.AplException;

public interface AccountControlPhasingService {

    AccountControlPhasing get(long accountId);

    int getCount();

    DbIterator<AccountControlPhasing> getAll(int from, int to);

    Stream<AccountControlPhasing> getAllStream(int from, int to);

    void unset(Account account);

    void set(Account senderAccount, SetPhasingOnly attachment);

    void checkTransaction(Transaction transaction) throws AplException.NotCurrentlyValidException;

    boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates);

    boolean isBlockDuplicate(Transaction transaction,
                             Map<TransactionType, Map<String, Integer>> duplicates,
                             Set<AccountControlType> senderAccountControls,
                             AccountControlPhasing accountControlPhasing);
}
