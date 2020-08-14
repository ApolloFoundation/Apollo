/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;

public interface AppendixApplier<T extends Appendix> {
    /**
     * Should perform state transition action for specific appendix assuming correct input state of appendix
     * @param transaction transaction which contains given appendix
     * @param appendix appendix which should be applied
     * @param senderAccount transaction's sender account
     * @param recipientAccount transaction's recipient account, may be null
     */
    void apply(Transaction transaction, T appendix, Account senderAccount, Account recipientAccount);
}
