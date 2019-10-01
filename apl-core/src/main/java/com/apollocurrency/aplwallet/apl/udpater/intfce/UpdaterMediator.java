/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.udpater.intfce;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Version;

import java.sql.Connection;
import java.sql.ResultSet;

public interface UpdaterMediator {

    void shutdownApplication();

    void suspendBlockchain();

    void resumeBlockchain();

    boolean isUpdateTransaction(Transaction transaction);

    Version getWalletVersion();

    int getBlockchainHeight();

    boolean isShutdown();

    TransactionalDataSource getDataSource();

    Transaction loadTransaction(Connection connection, ResultSet rs) throws AplException.NotValidException;

}
