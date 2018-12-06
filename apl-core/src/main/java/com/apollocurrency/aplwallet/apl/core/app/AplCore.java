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

package com.apollocurrency.aplwallet.apl.core.app;


import com.apollocurrency.aplwallet.apl.util.AplException;
import static com.apollocurrency.aplwallet.apl.core.app.Constants.DEFAULT_PEER_PORT;
import static com.apollocurrency.aplwallet.apl.core.app.Constants.TESTNET_API_SSLPORT;
import static com.apollocurrency.aplwallet.apl.core.app.Constants.TESTNET_PEER_PORT;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import com.apollocurrency.aplwallet.apl.core.addons.AddOns;
import com.apollocurrency.aplwallet.apl.cdi.AplContainer;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainIdService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.env.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apl.util.env.ServerStatus;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIProxy;
import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import com.apollocurrency.aplwallet.apl.util.ThreadPool;
import org.h2.jdbc.JdbcSQLException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AplCore {
    private static final Logger LOG = LoggerFactory.getLogger(AplCore.class);


    private static AplContainer container;
    private static ChainIdService chainIdService;
    public static final Version VERSION = Version.from("1.23.0");

    public static final String APPLICATION = "Apollo";

    private static volatile Time time = new Time.EpochTime();

    public static  RuntimeMode runtimeMode;
    public static  DirProvider dirProvider;

    public static RuntimeMode getRuntimeMode() {
        return runtimeMode;
    }

    private static volatile boolean shutdown = false;

    public static boolean isShutdown() {
        return shutdown;
    }
 
    public static boolean isDesktopApplicationEnabled() {
        return RuntimeEnvironment.isDesktopApplicationEnabled() && AplCore.getBooleanProperty("apl.launchDesktopApplication");
    }
    
    private static void logSystemProperties() {
        String[] loggedProperties = new String[] {
                "java.version",
                "java.vm.version",
                "java.vm.name",
                "java.vendor",
                "java.vm.vendor",
                "java.home",
                "java.library.path",
                "java.class.path",
                "os.arch",
                "sun.arch.data.model",
                "os.name",
                "file.encoding",
                "java.security.policy",
                "java.security.manager",
                RuntimeEnvironment.RUNTIME_MODE_ARG,
                RuntimeEnvironment.DIRPROVIDER_ARG
        };
        for (String property : loggedProperties) {
            LOG.debug("{} = {}", property, System.getProperty(property));
        }
        LOG.debug("availableProcessors = {}", Runtime.getRuntime().availableProcessors());
        LOG.debug("maxMemory = {}", Runtime.getRuntime().maxMemory());
        LOG.debug("processId = {}", getProcessId());
    }

    public static File getLogDir() {
        return dirProvider.getLogFileDir();
    }


    public static int getIntProperty(String name, int defaultValue) {
        return AplGlobalObjects.getPropertiesLoader().getIntProperty(name, defaultValue);
    }

    public static String getStringProperty(String name) {
        return getStringProperty(name, null, false);
    }

    public static String getStringProperty(String name, String defaultValue) {
        return getStringProperty(name, defaultValue, false);
    }

    public static String getStringProperty(String name, String defaultValue, boolean doNotLog) {
        return getStringProperty(name, defaultValue, doNotLog, null);
    }

    public static String getStringProperty(String name, String defaultValue, boolean doNotLog, String encoding) {
        return AplGlobalObjects.getPropertiesLoader().getStringProperty(name, defaultValue, doNotLog, encoding);
    }

    public static List<String> getStringListProperty(String name) {
        String value = getStringProperty(name);
        if (value == null || value.length() == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String s : value.split(";")) {
            s = s.trim();
            if (s.length() > 0) {
                result.add(s);
            }
        }
        return result;
    }

    public static int getIntProperty(String name) {
        return getIntProperty(name, 0);
    }
    
    public static boolean getBooleanProperty(String name) {
        return getBooleanProperty(name, false);
    }

    public static boolean getBooleanProperty(String name, boolean defaultValue) {
        return AplGlobalObjects.getPropertiesLoader().getBooleanProperty(name, defaultValue);
    }

    public static Blockchain getBlockchain() {
        return BlockchainImpl.getInstance();
    }

    public static BlockchainProcessor getBlockchainProcessor() {
        return BlockchainProcessorImpl.getInstance();
    }

    public static TransactionProcessor getTransactionProcessor() {
        return TransactionProcessorImpl.getInstance();
    }

    public static Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountATM, long feeATM, short deadline, Attachment attachment) {
        return new TransactionImpl.BuilderImpl((byte)1, senderPublicKey, amountATM, feeATM, deadline, (Attachment.AbstractAttachment)attachment);
    }

    public static Transaction.Builder newTransactionBuilder(byte[] transactionBytes) throws AplException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes);
    }

    public static Transaction.Builder newTransactionBuilder(JSONObject transactionJSON) throws AplException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionJSON);
    }

    public static Transaction.Builder newTransactionBuilder(byte[] transactionBytes, JSONObject prunableAttachments) throws AplException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes, prunableAttachments);
    }

    public static int getEpochTime() {
        return time.getTime();
    }

    static void setTime(Time time) {
        AplCore.time = time;
    }


    public static void init() {
        runtimeMode = RuntimeEnvironment.getRuntimeMode();
        System.out.printf("Runtime mode %s\n", runtimeMode.getClass().getName());
        dirProvider = RuntimeEnvironment.getDirProvider();
        System.out.println("User home folder " + dirProvider.getUserHomeDir());
        AplGlobalObjects.createPropertiesLoader(dirProvider);
        if (!VERSION.equals(Version.from(AplGlobalObjects.getPropertiesLoader().getDefaultProperties().getProperty("apl.version")))) {
            throw new RuntimeException("Using an apl-default.properties file from a version other than " + VERSION + " is not supported!!!");
        }        
        Init.init();
    }

    public static void shutdown() {
        LOG.info("Shutting down...");
        AddOns.shutdown();
        API.shutdown();
        FundingMonitor.shutdown();
        ThreadPool.shutdown();
        BlockchainProcessorImpl.getInstance().shutdown();
        Peers.shutdown();
        Db.shutdown();
        LOG.info(AplCore.APPLICATION + " server " + VERSION + " stopped.");
        runtimeMode.shutdown();
        AplCore.shutdown = true;
    }


    private static class Init {

        private static volatile boolean initialized = false;

        static {
            try {
                long startTime = System.currentTimeMillis();
                AplGlobalObjects.createNtpTime();
                PropertiesLoader propertiesLoader = AplGlobalObjects.getPropertiesLoader();
                AplGlobalObjects.createChainIdService(propertiesLoader.getStringProperty("apl.chainIdFilePath" , "chains.json"));
                AplGlobalObjects.createBlockchainConfig(AplGlobalObjects.getChainIdService().getActiveChain(), propertiesLoader, false);
                AplGlobalObjects.getChainConfig().init();
                propertiesLoader.loadSystemProperties(
                        Arrays.asList(
                                "socksProxyHost",
                                "socksProxyPort",
                                "apl.enablePeerUPnP"));
                logSystemProperties();
                runtimeMode.init();
                Thread secureRandomInitThread = initSecureRandom();
                runtimeMode.updateAppStatus("Database initialization...");

                checkPorts();
                setServerStatus(ServerStatus.BEFORE_DATABASE, null);
                Db.init();
                container = AplContainer.builder().containerId("MAIN-APL-CDI")
                        .annotatedDiscoveryMode().build();
                ChainIdDbMigration.migrate();
                setServerStatus(ServerStatus.AFTER_DATABASE, null);
                AplGlobalObjects.getChainConfig().updateToLatestConstants();
                TransactionProcessorImpl.getInstance();
                BlockchainProcessorImpl.getInstance();
                Account.init();
                AccountRestrictions.init();
                runtimeMode.updateAppStatus("Account ledger initialization...");
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
                runtimeMode.updateAppStatus("Peer server initialization...");
                Peers.init();
                runtimeMode.updateAppStatus("API Proxy initialization...");
                APIProxy.init();
                Generator.init();
                AddOns.init();
                runtimeMode.updateAppStatus("API initialization...");
                API.init();
                DebugTrace.init();
                int timeMultiplier = (AplGlobalObjects.getChainConfig().isTestnet() && Constants.isOffline) ? Math.max(AplCore.getIntProperty("apl.timeMultiplier"), 1) : 1;
                ThreadPool.start(timeMultiplier);
                if (timeMultiplier > 1) {
                    setTime(new Time.FasterTime(Math.max(getEpochTime(), AplCore.getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
                    LOG.info("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
                }
                try {
                    secureRandomInitThread.join(10000);
                }
                catch (InterruptedException ignore) {}
                testSecureRandom();
                long currentTime = System.currentTimeMillis();
                LOG.info("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
                String message = AplCore.APPLICATION + " server " + VERSION + " started successfully.";
                LOG.info(message);
                runtimeMode.updateAppStatus(message);
                LOG.info("Copyright © 2013-2016 The NXT Core Developers.");
                LOG.info("Copyright © 2016-2017 Jelurida IP B.V..");
                LOG.info("Copyright © 2017-2018 Apollo Foundation.");
                LOG.info("See LICENSE.txt for more information");
                if (API.getWelcomePageUri() != null) {
                    LOG.info("Client UI is at " + API.getWelcomePageUri());
                }
                setServerStatus(ServerStatus.STARTED, API.getWelcomePageUri());

                if (AplGlobalObjects.getChainConfig().isTestnet()) {
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
                runtimeMode.recoverDb();
            }
            catch (Exception e) {
                LOG.error(e.getMessage(), e);
                runtimeMode.alert(e.getMessage() + "\n" +
                        "See additional information in " + dirProvider.getLogFileDir() + System.getProperty("file.separator") + "apl.log");
                System.exit(1);
            }
        }

        public static void checkPorts() {
            Set<Integer> ports = collectWorkingPorts();
            for (Integer port : ports) {
                if (!isTcpPortAvailable(port)) {
                    String portErrorMessage = "Port " + port + " is already in use. Please, shutdown all Apollo processes and restart application!";
                    runtimeMode.displayError("ERROR!!! " + portErrorMessage);
                    throw new RuntimeException(portErrorMessage);
                }
            }
        }

        static Set<Integer> collectWorkingPorts() {
            boolean testnet = AplGlobalObjects.getChainConfig().isTestnet();
            final int port = testnet ?  Constants.TESTNET_API_PORT: AplCore.getIntProperty("apl.apiServerPort");
            final int sslPort = testnet ? TESTNET_API_SSLPORT : AplCore.getIntProperty("apl.apiServerSSLPort");
            boolean enableSSL = AplCore.getBooleanProperty("apl.apiSSL");
            int peerPort = -1;

            String myAddress = Convert.emptyToNull(AplCore.getStringProperty("apl.myAddress", "").trim());
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
            int peerServerPort = AplCore.getIntProperty("apl.peerServerPort");

            Set<Integer> ports = new HashSet<>();
            ports.add(port);
            if (enableSSL) {
                ports.add(sslPort);
            }
            ports.add(peerPort);
            ports.add(testnet ? TESTNET_PEER_PORT : peerServerPort);
            return ports;
        }

        public static boolean isTcpPortAvailable(int port) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.setReuseAddress(true);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }



        private static void init() {
            if (initialized) {
                throw new RuntimeException("Apl.init has already been called");
            }
            initialized = true;
        }

        private Init() {} // never

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


    public static String getProcessId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        if (runtimeName == null) {
            return "";
        }
        String[] tokens = runtimeName.split("@");
        if (tokens.length == 2) {
            return tokens[0];
        }
        return "";
    }

    public static String getDbDir(String dbDir, UUID chainId, boolean chainIdFirst) {
        return dirProvider.getDbDir(dbDir, chainId, chainIdFirst);
    }

    public static String getDbDir(String dbDir, boolean chainIdFirst) {
        return dirProvider.getDbDir(dbDir, AplGlobalObjects.getChainConfig().getChain().getChainId(), chainIdFirst);
    }

    public static String getDbDir(String dbDir) {
        return dirProvider.getDbDir(dbDir, AplGlobalObjects.getChainConfig().getChain().getChainId(), false);
    }

    public static Path getKeystoreDir(String keystoreDir) {
        return dirProvider.getKeystoreDir(keystoreDir).toPath();
    }

    public static Path get2FADir(String dir2FA) {
        return Paths.get(dirProvider.getUserHomeDir(), dir2FA);
    }


    public static void updateLogFileHandler(Properties loggingProperties) {
        dirProvider.updateLogFileHandler(loggingProperties);
    }

    public static String getUserHomeDir() {
        return dirProvider.getUserHomeDir();
    }

    public static File getConfDir() {
        return dirProvider.getConfDir();
    }

    private static void setServerStatus(ServerStatus status, URI wallet) {
        runtimeMode.setServerStatus(status, wallet, dirProvider.getLogFileDir());
    }

    
//TODO: Core should not be statis anymore!
    
    public AplCore() {
    }

}
