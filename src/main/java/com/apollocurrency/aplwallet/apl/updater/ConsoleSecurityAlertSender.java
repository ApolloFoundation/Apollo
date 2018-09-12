/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.util.Logger;

public class ConsoleSecurityAlertSender implements SecurityAlertSender {
    @Override
    public void send(Transaction invalidUpdateTransaction) {
        Logger.logInfoMessage("Transaction: " + invalidUpdateTransaction.getJSONObject().toJSONString() + " is invalid");
    }
    @Override
    public void send(String message) {
        Logger.logWarningMessage(message);
    }
}
