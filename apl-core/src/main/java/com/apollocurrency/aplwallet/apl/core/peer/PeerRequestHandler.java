/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessorImpl;
import javax.enterprise.inject.spi.CDI;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 *
 * @author al
 */
abstract class PeerRequestHandler {
    
    abstract JSONStreamAware processRequest(JSONObject request, Peer peer);

    abstract boolean rejectWhileDownloading();

    protected boolean isChainIdProtected() {
        return true;
    }
    private Blockchain blockchain;
    private BlockchainProcessor blockchainProcessor;
    private TransactionProcessor transactionProcessor;

    protected Blockchain lookupBlockchain() {
        if (blockchain == null) {
            blockchain = CDI.current().select(BlockchainImpl.class).get();
        }
        return blockchain;
    }

    protected BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        }
        return blockchainProcessor;
    }

    protected TransactionProcessor lookupTransactionProcessor() {
        if (transactionProcessor == null) {
            transactionProcessor = CDI.current().select(TransactionProcessorImpl.class).get();
        }
        return transactionProcessor;
    }
    
}
