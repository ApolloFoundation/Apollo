/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.udpater.intfce;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

public interface UpdaterMediator {

    void shutdownApplication();

    void suspendBlockchain();

    void resumeBlockchain();

    boolean isUpdateTransaction(Transaction transaction);

    Version getWalletVersion();

    int getBlockchainHeight();

    boolean isShutdown();

    TransactionalDataSource getDataSource();

    Transaction getTransaction(long id);

    PropertiesHolder getPropertyHolder();

    public String getChainId();

}
