/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package apl.updater;

import apl.Transaction;
import apl.util.Logger;

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

    private SecurityAlertSender() {
    }

    public void send(String message) {
        Logger.logWarningMessage(message);
    }
}
