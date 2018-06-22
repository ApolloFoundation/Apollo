package apl.updater;

import apl.Transaction;
import apl.util.Logger;

public class SecurityAlertSender {
    public void send(Transaction invalidUpdateTransaction) {
        Logger.logInfoMessage("Transaction: " + invalidUpdateTransaction.getJSONObject().toJSONString() + " is invalid");
    }
}
