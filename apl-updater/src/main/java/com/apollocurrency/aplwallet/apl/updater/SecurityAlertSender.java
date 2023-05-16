/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;

public interface SecurityAlertSender {

    void send(Transaction invalidUpdateTransaction);

    void send(String message);
}
