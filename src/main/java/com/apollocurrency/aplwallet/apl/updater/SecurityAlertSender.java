/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.Transaction;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class SecurityAlertSender {
    private static final Logger LOG = getLogger(SecurityAlertSender.class);

    private static class SecurityAlertSenderHolder {
        private static final SecurityAlertSender INSTANCE = new SecurityAlertSender();
    }

    public static SecurityAlertSender getInstance() {
        return SecurityAlertSenderHolder.INSTANCE;
    }

    public void send(Transaction invalidUpdateTransaction) {
        LOG.info("Transaction: " + invalidUpdateTransaction.getJSONObject().toJSONString() + " is invalid");
    }
    private SecurityAlertSender() {}

    public void send(String message) {
        LOG.warn(message);
    }
}
