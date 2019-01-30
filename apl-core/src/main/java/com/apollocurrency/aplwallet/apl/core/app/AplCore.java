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


import static com.apollocurrency.aplwallet.apl.core.app.Constants.DEFAULT_PEER_PORT;
import static com.apollocurrency.aplwallet.apl.core.app.Constants.TESTNET_API_SSLPORT;
import static com.apollocurrency.aplwallet.apl.core.app.Constants.TESTNET_PEER_PORT;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.addons.AddOns;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIProxy;
import com.apollocurrency.aplwallet.apl.core.migrator.ApplicationDataMigrationManager;
import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import com.apollocurrency.aplwallet.apl.core.rest.filters.ApiSplitFilter;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.AppStatus;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.ThreadPool;
import com.apollocurrency.aplwallet.apl.util.UPnP;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeParams;
import com.apollocurrency.aplwallet.apl.util.env.ServerStatus;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.h2.jdbc.JdbcSQLException;
import org.slf4j.Logger;

import java.net.URI;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.spi.CDI;

public final class AplCore {
    private static Logger LOG;// = LoggerFactory.getLogger(AplCore.class);
    
//those vars needed to just pull CDI to crerate it befor we gonna use it in threads
    private AbstractBlockValidator bcValidator;
    
    private static volatile boolean shutdown = false;

    private static volatile Time time = CDI.current().select(Time.EpochTime.class).get();
    private PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    private BlockchainConfig blockchainConfig;
    private static Blockchain blockchain;
    private static BlockchainProcessor blockchainProcessor;


    public AplCore(BlockchainConfig config) {
        this.blockchainConfig = config;
    }
    
    public static boolean isShutdown() {
        return shutdown;
    }
 

    public static int getEpochTime() { // left for awhile
        return time.getTime();
    }

    static void setTime(Time time) { // left for awhile
        AplCore.time = time;
    }


    public void init() {

        System.out.printf("Runtime mode %s\n", AplCoreRuntime.getInstance().getRuntimeMode().getClass().getName());
        // dirProvider = RuntimeEnvironment.getDirProvider();
        LOG = getLogger(AplCore.class);
        LOG.debug("User home folder '{}'", AplCoreRuntime.getInstance().getDirProvider().getAppBaseDir());
//TODO: Do we really need this check?        
//        if (!Constants.VERSION.equals(Version.from(propertiesHolder.getStringProperty("apl.version")))) {
//            LOG.warn("Versions don't match = {} and {}", Constants.VERSION, propertiesHolder.getStringProperty("apl.version"));
//            throw new RuntimeException("Using an apl-default.properties file from a version other than " + Constants.VERSION + " is not supported!!!");
//        }
        startUp();
    }

    public void shutdown() {
        LOG.info("Shutting down...");
        AddOns.shutdown();
        API.shutdown();
        FundingMonitor.shutdown();
        ThreadPool.shutdown();
        blockchainProcessor.shutdown();
        Peers.shutdown();
        Db.shutdown();
        LOG.info(Constants.APPLICATION + " server " + Constants.VERSION + " stopped.");
        AplCore.shutdown = true;
    }
    
    private static void setServerStatus(ServerStatus status, URI wallet) {
        AplCoreRuntime.getInstance().setServerStatus(status, wallet);
    }

    private static volatile boolean initialized = false;


    private void startUp() {

        if (initialized) {
            throw new RuntimeException("Apl.init has already been called");
        }
        initialized = true;


            try {
                long startTime = System.currentTimeMillis();
                checkPorts();  
                //try to start API as early as possible
                API.init();
                
                bcValidator = CDI.current().select(DefaultBlockValidator.class).get();
                CDI.current().select(NtpTime.class).get().start();
                                
//TODO: check, may be we still need this 
//                this.blockchainConfig = CDI.current().select(BlockchainConfig.class).get();

                AplCoreRuntime.logSystemProperties();
                Thread secureRandomInitThread = initSecureRandom();
                AppStatus.getInstance().update("Database initialization...");


                setServerStatus(ServerStatus.BEFORE_DATABASE, null);

                Db.init();
                ApplicationDataMigrationManager migrationManager = CDI.current().select(ApplicationDataMigrationManager.class).get();
                migrationManager.executeDataMigration();

                setServerStatus(ServerStatus.AFTER_DATABASE, null);

                blockchainConfig.registerConfigChangeListener(); // create inside Apollo and passed into AplCore constructor
                blockchainConfig.updateToLatestConfig();

               
                //TODO: move to application level this UPnP initialization
                boolean enablePeerUPnP = propertiesHolder.getBooleanProperty("apl.enablePeerUPnP");
                boolean enableAPIUPnP = propertiesHolder.getBooleanProperty("apl.enableAPIUPnP");
                if(enableAPIUPnP || enablePeerUPnP){
                    UPnP.TIMEOUT = propertiesHolder.getIntProperty("apl.upnpDiscoverTimeout",3000);
                    UPnP.getInstance();
                }
                TransactionProcessor transactionProcessor = CDI.current().select(TransactionProcessor.class).get();
                blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
                blockchain = CDI.current().select(BlockchainImpl.class).get();
                transactionProcessor.init();

                Account.init();
                AccountRestrictions.init();
                AppStatus.getInstance().update("Account ledger initialization...");
                AccountLedger.init();
                Alias.init();
                Asset.init();
                DigitalGoodsStore.init();
                Order.init();
                Poll.init();
                PhasingPoll.init();
                Trade.init();
                AssetTransfer.init();
                AssetDelete.init();
                AssetDividend.init();
                Vote.init();
                PhasingVote.init();
                Currency.init();
                CurrencyExchangeOffer.init();
                CurrencyBuyOffer.init();
                CurrencySellOffer.init();
                CurrencyFounder.init();
                CurrencyMint.init();
                CurrencyTransfer.init();
                Exchange.init();
                ExchangeRequest.init();
                Shuffling.init();
                ShufflingParticipant.init();
                PrunableMessage.init();
                TaggedData.init();
                AppStatus.getInstance().update("Peer server initialization...");
                Peers.init();
                AppStatus.getInstance().update("API Proxy initialization...");
                APIProxy.init();
                Generator.init();
                AddOns.init();
                AppStatus.getInstance().update("API initialization...");
                DebugTrace.init();
//signal to API that core is reaqdy to serve requests. Should be removed as soon as all API will be on RestEasy                
                ApiSplitFilter.isCoreReady = true;
                int timeMultiplier = (blockchainConfig.isTestnet() && Constants.isOffline) ? Math.max(propertiesHolder.getIntProperty("apl" +
                        ".timeMultiplier"), 1) : 1;
                ThreadPool.start(timeMultiplier);
                if (timeMultiplier > 1) {
                    setTime(new Time.FasterTime(Math.max(getEpochTime(), blockchain.getLastBlock().getTimestamp()), timeMultiplier));
                    LOG.info("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
                }
                try {
                    secureRandomInitThread.join(10000);
                }
                catch (InterruptedException ignore) {}
                testSecureRandom();
                long currentTime = System.currentTimeMillis();
                LOG.info("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
                String message = Constants.APPLICATION + " server " + Constants.VERSION + " started successfully.";
                LOG.info(message);
                AppStatus.getInstance().update(message);
                LOG.info("Copyright © 2013-2016 The NXT Core Developers.");
                LOG.info("Copyright © 2016-2017 Jelurida IP B.V..");
                LOG.info("Copyright © 2017-2019 Apollo Foundation.");
                LOG.info("See LICENSE.txt for more information");
                if (API.getWelcomePageUri() != null) {
                    LOG.info("Client UI is at " + API.getWelcomePageUri());
                }
                setServerStatus(ServerStatus.STARTED, API.getWelcomePageUri());

                if (blockchainConfig.isTestnet()) {
                    LOG.info("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
                }
            }
            catch (final RuntimeException e) {
                if (e.getMessage() == null || (!e.getMessage().contains(JdbcSQLException.class.getName()) && !e.getMessage().contains(SQLException.class.getName()))) {
                    Throwable exception = e;
                    while (exception.getCause() != null) { //get root cause of RuntimeException
                        exception = exception.getCause();
                    }
                    if (exception.getClass() != JdbcSQLException.class && exception.getClass() != SQLException.class) {
                        throw e; //re-throw non-db exception
                    }
                }
                LOG.error("Database initialization failed ", e);
                //TODO: move DB operations to proper place
                // AplCoreRuntime.getInstance().getRuntimeMode().recoverDb();
            }
            catch (Exception e) {
                LOG.error(e.getMessage(), e);
                AppStatus.getInstance().alert(e.getMessage() + "\n" +
                        "See additional information in log files");
                System.exit(1);
            }
        }
        void checkPorts() {
            Set<Integer> ports = collectWorkingPorts();
            for (Integer port : ports) {
                if (!RuntimeParams.isTcpPortAvailable(port)) {
                    String portErrorMessage = "Port " + port + " is already in use. Please, shutdown all Apollo processes and restart application!";
                    AppStatus.getInstance().error("ERROR!!! " + portErrorMessage);
                    throw new RuntimeException(portErrorMessage);
                }
            }
        }

        private Set<Integer> collectWorkingPorts() {
            boolean testnet = blockchainConfig.isTestnet();
            final int port = testnet ?  Constants.TESTNET_API_PORT: propertiesHolder.getIntProperty("apl.apiServerPort");
            final int sslPort = testnet ? TESTNET_API_SSLPORT : propertiesHolder.getIntProperty("apl.apiServerSSLPort");
            boolean enableSSL = propertiesHolder.getBooleanProperty("apl.apiSSL");
            int peerPort = -1;

            String myAddress = Convert.emptyToNull(propertiesHolder.getStringProperty("apl.myAddress", "").trim());
            if (myAddress != null) {
                try {
                    int portIndex = myAddress.lastIndexOf(":");
                    if (portIndex != -1) {
                        peerPort = Integer.parseInt(myAddress.substring(portIndex + 1));
                    }
                }
                catch (NumberFormatException e) {
                    LOG.error("Unable to parse port in '{}' address",myAddress);
                }
            }
            if (peerPort == -1) {
                peerPort = testnet ? TESTNET_PEER_PORT : DEFAULT_PEER_PORT;
            }
            int peerServerPort = propertiesHolder.getIntProperty("apl.peerServerPort");

            Set<Integer> ports = new HashSet<>();
            ports.add(port);
            if (enableSSL) {
                ports.add(sslPort);
            }
            ports.add(peerPort);
            ports.add(testnet ? TESTNET_PEER_PORT : peerServerPort);
            return ports;
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
        } catch (InterruptedException ignore) {}
    }

}
