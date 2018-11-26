/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.Transaction;

public interface SecurityAlertSender {

    void send(Transaction invalidUpdateTransaction);

    void send(String message);
}
