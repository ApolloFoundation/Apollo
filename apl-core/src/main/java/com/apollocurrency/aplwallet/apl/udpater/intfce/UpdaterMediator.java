/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.udpater.intfce;

import com.apollocurrency.aplwallet.apl.util.ConnectionProvider;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.Version;
import com.apollocurrency.aplwallet.apl.core.app.Version;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Listener;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

public interface UpdaterMediator {

    void shutdownApplication();

    void suspendBlockchain();

    void resumeBlockchain();

    void addUpdateListener(Listener<List<? extends Transaction>> listener);

    void removeUpdateListener(Listener<List<? extends Transaction>> listener);

    boolean isUpdateTransaction(Transaction transaction);

    Version getWalletVersion();

    int getBlockchainHeight();

    boolean isShutdown();

    ConnectionProvider getConnectionProvider();

    Transaction loadTransaction(Connection connection, ResultSet rs) throws AplException.NotValidException;

}
