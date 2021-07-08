/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.BlockchainProcessorState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetNextBlocksResponseParser;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetTransactionsResponseParser;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockSerializer;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableRestorationService;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class GetMoreBlocksThreadTest {
    @Mock
    BlockchainProcessor blockchainProcessor;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    Blockchain blockchain;
    @Mock
    PeersService peersService;
    @Mock
    GlobalSync globalSync;
    @Mock
    TimeService timeService;
    @Mock
    PrunableRestorationService prunableRestorationService;
    @Mock
    ExecutorService networkService;
    @Mock
    TransactionProcessor transactionProcessor;
    @Mock
    PropertiesHolder propertiesHolder;
    @Mock
    BlockSerializer blockSerializer;
    @Mock
    GetNextBlocksResponseParser getNextBlocksResponseParser;
    @Mock
    GetTransactionsResponseParser getTransactionsResponseParser;
    @Mock
    Chain chain;

    GetMoreBlocksThread thread;
    BlockchainProcessorState state = new BlockchainProcessorState();

    @BeforeEach
    void setUp() {

        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(UUID.randomUUID()).when(chain).getChainId();
        thread = new GetMoreBlocksThread(blockchainProcessor, state, blockchainConfig, blockchain, peersService
            , globalSync, timeService, prunableRestorationService, networkService, propertiesHolder, transactionProcessor, getNextBlocksResponseParser, blockSerializer, getTransactionsResponseParser);
    }

    @Test
    void run() {
    }
}