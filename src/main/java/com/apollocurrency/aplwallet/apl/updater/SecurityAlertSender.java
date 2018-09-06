/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.util.Logger;

public class SecurityAlertSender {
    private static class SecurityAlertSenderHolder {
        private static final SecurityAlertSender INSTANCE = new SecurityAlertSender();
    }

    public static SecurityAlertSender getInstance() {
        return SecurityAlertSenderHolder.INSTANCE;
    }

    public void send(Transaction invalidUpdateTransaction) {
        Logger.logInfoMessage("Transaction: " + invalidUpdateTransaction.getJSONObject().toJSONString() + " is invalid");
    }
    private SecurityAlertSender() {}

    public void send(String message) {
        Logger.logWarningMessage(message);
    }
}
