/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;

public interface PrunableLoader<T extends Appendix> {

    void loadPrunable(Transaction transaction, T appendix, boolean includeExpiredPrunable);

    void restorePrunableData(Transaction transaction, T appendix, int blockTimestamp, int height);

    /**
     * @return class instance for which prunable loading has to be performed
     */
    Class<T> forClass();

}
