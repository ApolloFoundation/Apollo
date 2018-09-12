/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.Transaction;

public interface UpdateTransactionVerifier {
    UpdateData process(Transaction transaction);
}
