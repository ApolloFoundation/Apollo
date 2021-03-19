/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

public abstract class AbstractAppendixValidator<T extends Appendix> implements AppendixValidator<T> {
    @Override
    public void validateAtFinish(Transaction transaction, T phasingAppendix, int blockHeight) throws AplException.ValidationException {
        if (!phasingAppendix.isPhased(transaction)) {
            return;
        }
        validateStateIndependent(transaction, phasingAppendix, blockHeight);
        validateStateDependent(transaction, phasingAppendix, blockHeight);
    }
}
