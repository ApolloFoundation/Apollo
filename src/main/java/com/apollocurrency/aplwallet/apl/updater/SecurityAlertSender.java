/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
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
