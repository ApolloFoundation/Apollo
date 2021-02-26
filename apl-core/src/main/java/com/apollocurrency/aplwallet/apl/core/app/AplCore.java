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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;


import com.apollocurrency.aplwallet.apl.core.addons.AddOns;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIProxy;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.rest.filters.ApiSplitFilter;
import com.apollocurrency.aplwallet.apl.core.rest.service.TransportInteractionService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.AbstractBlockValidator;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.DefaultBlockValidator;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessingTaskScheduler;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.service.state.TableRegistryInitializer;
import com.apollocurrency.aplwallet.apl.core.shard.PrunableArchiveMonitor;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.core.transaction.TxInitializer;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.exchange.service.DexOperationService;
import com.apollocurrency.aplwallet.apl.exchange.service.DexOrderProcessor;
import com.apollocurrency.aplwallet.apl.exchange.service.IDexMatcherInterface;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.UPnP;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeParams;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static com.apollocurrency.aplwallet.apl.util.Constants.DEFAULT_PEER_PORT;

@Slf4j
public final class AplCore {

    private static volatile boolean shutdown = false;
    //    private static BlockchainConfig blockchainConfig;
    private static TransportInteractionService transportInteractionService;
    private /*static*/ volatile boolean initialized = false;
    @Inject
    @Setter
    PeersService peers;
    @Inject
    @Setter
    TableRegistryInitializer tableRegistryInitializer;
    @Inject
    @Setter
    DerivedTablesRegistry dbRegistry;
    //those vars needed to just pull CDI to crerate it befor we gonna use it in threads
    private AbstractBlockValidator bcValidator;
    private TimeService time;
    private Blockchain blockchain;
    private BlockchainProcessor blockchainProcessor;
    private DatabaseManager databaseManager;
    //private FullTextSearchService fullTextSearchService;
    private API apiServer;
    private IDexMatcherInterface tcs;
    @Inject
    @Setter
    private PropertiesHolder propertiesHolder;
    @Inject
    @Setter
    private DirProvider dirProvider;
    @Inject
    @Setter
    private AplAppStatus aplAppStatus;
    @Inject
    @Setter
    private TaskDispatchManager taskDispatchManager;

    private String initCoreTaskID;

    public AplCore() {
        time = CDI.current().select(TimeService.class).get();
    }

    public static boolean isShutdown() {
        return shutdown;
    }

    private static Thread initSecureRandom() {
        Thread secureRandomInitThread = new Thread(() -> Crypto.getSecureRandom().nextBytes(new byte[1024]));
        secureRandomInitThread.setDaemon(true);
        secureRandomInitThread.start();
        return secureRandomInitThread;
    }

    private static void testSecureRandom() {
        Thread thread = new Thread(() -> Crypto.getSecureRandom().nextBytes(new byte[1024]));
        thread.setDaemon(true);
        thread.start();
        try {
            thread.join(2000);
            if (thread.isAlive()) {
                throw new RuntimeException("SecureRandom implementation too slow!!! " +
                    "Install haveged if on linux, or set apl.useStrongSecureRandom=false.");
            }
        } catch (InterruptedException ignore) {
        }
    }

    public void init() {
        log.debug("Application home folder '{}'", dirProvider.getAppBaseDir());
        initCoreTaskID = aplAppStatus.durableTaskStart("AplCore Init",
            "Apollo core initialization task", true);
        startUp();
    }

    public void shutdown() {
        log.info("Shutting down...");
        AddOns.shutdown();
        if (apiServer != null) apiServer.shutdown();
//        FundingMonitor.shutdown();
        log.info("Background tasks shutdown...");
        if (taskDispatchManager != null) taskDispatchManager.shutdown();

        if (blockchainProcessor != null) {
            blockchainProcessor.shutdown();
            log.info("blockchainProcessor Shutdown...");
        }
//        if (fullTextSearchService != null) fullTextSearchService.shutdown();
//        log.info("full text service shutdown...");

        if (databaseManager != null) {
            databaseManager.shutdown();
            log.info("databaseManager Shutdown...");
        }

        if (transportInteractionService != null) {
            log.info("transport interaction service shutdown...");
            transportInteractionService.stop();
        }

        if (peers != null) peers.shutdown();
        log.info(Constants.APPLICATION + " server " + Constants.VERSION + " stopped.");

        AplCore.shutdown = true;

        if (tcs != null) tcs.deinitialize();
    }

    private void startUp() {

        if (initialized) {
            throw new RuntimeException("Apl.init has already been called");
        }
        initialized = true;


        try {
            long startTime = System.currentTimeMillis();
            checkPorts();
            //TODO: move to application level this UPnP initialization
            boolean enablePeerUPnP = propertiesHolder.getBooleanProperty("apl.enablePeerUPnP");
            boolean enableAPIUPnP = propertiesHolder.getBooleanProperty("apl.enableAPIUPnP");
            if (enableAPIUPnP || enablePeerUPnP) {
                UPnP.TIMEOUT = propertiesHolder.getIntProperty("apl.upnpDiscoverTimeout", 3000);
                UPnP upnp = CDI.current().select(UPnP.class).get();
                String upnpTid = aplAppStatus.durableTaskStart("UPnP init", "Tryin to get UPnP router", false);
                upnp.init();
                aplAppStatus.durableTaskFinished(upnpTid, false, "UPnP init done");
            }
            aplAppStatus.durableTaskUpdate(initCoreTaskID, 1.0, "API initialization");
            TxInitializer txInitializer = CDI.current().select(TxInitializer.class).get();
            //try to start API as early as possible
            apiServer = CDI.current().select(API.class).get();
            apiServer.start();
            aplAppStatus.durableTaskUpdate(initCoreTaskID, 5.0, "API initialization done");


            transportInteractionService = CDI.current().select(TransportInteractionService.class).get();
            transportInteractionService.start();
            aplAppStatus.durableTaskUpdate(initCoreTaskID, 5.5, "Transport control service initialization done");

            AplCoreRuntime.logSystemProperties();
            Thread secureRandomInitThread = initSecureRandom();
            aplAppStatus.durableTaskUpdate(initCoreTaskID, 6.0, "Database initialization");

            databaseManager = CDI.current().select(DatabaseManager.class).get();
            databaseManager.getDataSource();
            CDI.current().select(BlockchainConfigUpdater.class).get().updateToLatestConfig();
//            fullTextSearchService = CDI.current().select(FullTextSearchService.class).get();
//            fullTextSearchService.init(); // first time BEFORE migration
            aplAppStatus.durableTaskUpdate(initCoreTaskID, 30.0, "Database initialization done");
            aplAppStatus.durableTaskUpdate(initCoreTaskID, 30.1, "Apollo Data migration started");

            BlockchainConfigUpdater blockchainConfigUpdater = CDI.current().select(BlockchainConfigUpdater.class).get();
            blockchainConfigUpdater.updateToLatestConfig(); // update config for migrated db

            aplAppStatus.durableTaskUpdate(initCoreTaskID, 50.0, "Apollo Data migration done");

            databaseManager.getDataSource(); // retrieve again after migration to have it fresh for everyone

            aplAppStatus.durableTaskUpdate(initCoreTaskID, 50.1, "Apollo core cleaases initialization");


            aplAppStatus.durableTaskUpdate(initCoreTaskID, 52.5, "Exchange matcher initialization");

            tcs = CDI.current().select(IDexMatcherInterface.class).get();
            tcs.initialize();


            bcValidator = CDI.current().select(DefaultBlockValidator.class).get();
            blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
            blockchain = CDI.current().select(BlockchainImpl.class).get();
            blockchain.update();
            peers.init();
            GenesisAccounts.init();

            aplAppStatus.durableTaskUpdate(initCoreTaskID, 55.0, "Apollo Account ledger initialization");

            aplAppStatus.durableTaskUpdate(initCoreTaskID, 60.0, "Apollo Account ledger initialization done");
            aplAppStatus.durableTaskUpdate(initCoreTaskID, 61.0, "Apollo Peer services initialization started");
            APIProxy.init();
//            Generator.init();
            AddOns.init();
            aplAppStatus.durableTaskUpdate(initCoreTaskID, 70.1, "Apollo core classes initialization done");
            //signal to API that core is ready to serve requests. Should be removed as soon as all API will be on RestEasy
            ApiSplitFilter.isCoreReady = true;

            // start shard process recovery after initialization of all derived tables but before launching threads (blockchain downloading, transaction processing)
            recoverSharding();

            //Init classes to add tasks to the TaskDispatchManager
            CDI.current().select(DexOrderProcessor.class).get();
            CDI.current().select(PrunableArchiveMonitor.class).get();
            CDI.current().select(DexOperationService.class).get();
            CDI.current().select(TransactionProcessingTaskScheduler.class).get();

            //start all background tasks
            taskDispatchManager.dispatch();

            try {
                secureRandomInitThread.join(10000);
            } catch (InterruptedException ignore) {
            }
            testSecureRandom();

            if (log.isDebugEnabled()) {
                log.debug("AplCore setUp: {}", dbRegistry.toString());
            }

            long currentTime = System.currentTimeMillis();
            log.info("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
            String message = Constants.APPLICATION + " server " + Constants.VERSION + " started successfully.";
            aplAppStatus.durableTaskUpdate(initCoreTaskID, 100.0, message);
            log.info("Copyright © 2013-2016 The NXT Core Developers.");
            log.info("Copyright © 2016-2017 Jelurida IP B.V..");
            log.info("Copyright © 2017-2020 Apollo Foundation.");
            log.info("See LICENSE.txt for more information");
            if (API.getWelcomePageUri() != null) {
                log.info("Client UI is at " + API.getWelcomePageUri());
            }
            aplAppStatus.durableTaskFinished(initCoreTaskID, false, "AplCore initialized successfully");
        } catch (final RuntimeException e) {
            if (e.getMessage() == null || !e.getMessage().contains(SQLException.class.getName())) {
                Throwable exception = e;
                while (exception.getCause() != null) { //get root cause of RuntimeException
                    exception = exception.getCause();
                }
                if (exception.getClass() != SQLException.class) {
                    throw e; //re-throw non-db exception
                }
            }
            aplAppStatus.durableTaskFinished(initCoreTaskID, true, "AplCore init failed (DB)");
            log.error("Database initialization failed ", e);
            //TODO: move DB operations to proper place
            // AplCoreRuntime.getInstance().getRuntimeMode().recoverDb();
        } catch (Exception e) {
            aplAppStatus.durableTaskFinished(initCoreTaskID, true, "AplCore init failed");
            log.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    private void recoverSharding() {
        CDI.current().select(ShardService.class).get().recoverSharding();
    }

    void checkPorts() {
        Set<Integer> ports = collectWorkingPorts();
        for (Integer port : ports) {
            if (!RuntimeParams.isTcpPortAvailable(port)) {
                String portErrorMessage = "Port " + port + " is already in use. Please, shutdown all Apollo processes and restart application!";
                throw new RuntimeException(portErrorMessage);
            }
        }
    }

    private Set<Integer> collectWorkingPorts() {
        final int port = propertiesHolder.getIntProperty("apl.apiServerPort");
        final int sslPort = propertiesHolder.getIntProperty("apl.apiServerSSLPort");
        boolean enableSSL = propertiesHolder.getBooleanProperty("apl.apiSSL");
        int peerPort = -1;

        String myAddress = Convert.emptyToNull(propertiesHolder.getStringProperty("apl.myAddress", "").trim());
        if (myAddress != null) {
            try {
                int portIndex = myAddress.lastIndexOf(":");
                if (portIndex != -1) {
                    peerPort = Integer.parseInt(myAddress.substring(portIndex + 1));
                }
            } catch (NumberFormatException e) {
                log.error("Unable to parse port in '{}' address", myAddress);
            }
        }
        if (peerPort == -1) {
            peerPort = propertiesHolder.getIntProperty("apl.networkPeerServerPort", DEFAULT_PEER_PORT);
        }
        int peerServerPort = propertiesHolder.getIntProperty("apl.myPeerServerPort");

        Set<Integer> ports = new HashSet<>();
        ports.add(port);
        if (enableSSL) {
            ports.add(sslPort);
        }
        ports.add(peerPort);
        ports.add(peerServerPort);
        return ports;
    }

}
