/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateData;

public interface UpdateTransactionVerifier {
    UpdateData process(Transaction transaction);

    UpdateData process(Attachment attachment, long transactionId);
}
