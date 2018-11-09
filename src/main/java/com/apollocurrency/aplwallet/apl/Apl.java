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

package com.apollocurrency.aplwallet.apl;

import static com.apollocurrency.aplwallet.apl.Constants.DEFAULT_PEER_PORT;
import static com.apollocurrency.aplwallet.apl.Constants.TESTNET_API_SSLPORT;
import static com.apollocurrency.aplwallet.apl.Constants.TESTNET_PEER_PORT;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;


import com.apollocurrency.aplwallet.apl.dbmodel.Option;
import static com.apollocurrency.aplwallet.apl.Constants.DEFAULT_PEER_PORT;
import static com.apollocurrency.aplwallet.apl.Constants.TESTNET_API_SSLPORT;
import static com.apollocurrency.aplwallet.apl.Constants.TESTNET_PEER_PORT;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.addons.AddOns;
import com.apollocurrency.aplwallet.apl.chainid.Chain;
import com.apollocurrency.aplwallet.apl.chainid.ChainIdService;
import com.apollocurrency.aplwallet.apl.chainid.ChainIdServiceImpl;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.env.DirProvider;
import com.apollocurrency.aplwallet.apl.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.env.RuntimeMode;
import com.apollocurrency.aplwallet.apl.env.ServerStatus;
import com.apollocurrency.aplwallet.apl.http.API;
import com.apollocurrency.aplwallet.apl.http.APIProxy;
import com.apollocurrency.aplwallet.apl.peer.Peers;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.ThreadPool;
import com.apollocurrency.aplwallet.apl.util.Time;
import org.h2.jdbc.JdbcSQLException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;

public final class Apl {
    private static Logger LOG;
    private static ChainIdService chainIdService;
    public static final Version VERSION = Version.from("1.21.8");
    public static final String APPLICATION = "Apollo";
    private static Thread shutdownHook;
    private static volatile Time time = new Time.EpochTime();

    public static final String APL_DEFAULT_PROPERTIES = "apl-default.properties";
    public static final String APL_PROPERTIES = "apl.properties";
    public static final String APL_INSTALLER_PROPERTIES = "apl-installer.properties";
    public static final String CONFIG_DIR = "conf";

    private static final RuntimeMode runtimeMode;
    private static final DirProvider dirProvider;

    public static RuntimeMode getRuntimeMode() {
        return runtimeMode;
    }

    private static final Properties defaultProperties = new Properties();
    static {
        redirectSystemStreams("out");
        redirectSystemStreams("err");
        System.out.println("Initializing " + Apl.APPLICATION + " server version " + Apl.VERSION);
        printCommandLineArguments();
        runtimeMode = RuntimeEnvironment.getRuntimeMode();
        System.out.printf("Runtime mode %s\n", runtimeMode.getClass().getName());
        dirProvider = RuntimeEnvironment.getDirProvider();
        LOG = getLogger(Apl.class);
        System.out.println("User home folder " + dirProvider.getUserHomeDir());
        loadProperties(defaultProperties, APL_DEFAULT_PROPERTIES, true);
        if (!VERSION.equals(Version.from(Apl.defaultProperties.getProperty("apl.version")))) {
            throw new RuntimeException("Using an apl-default.properties file from a version other than " + VERSION + " is not supported!!!");
        }
    }

    private static volatile boolean shutdown = false;

    public static boolean isShutdown() {
        return shutdown;
    }

    private static void redirectSystemStreams(String streamName) {
        String isStandardRedirect = System.getProperty("apl.redirect.system." + streamName);
        Path path = null;
        if (isStandardRedirect != null) {
            try {
                path = Files.createTempFile("apl.system." + streamName + ".", ".log");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            String explicitFileName = System.getProperty("apl.system." + streamName);
            if (explicitFileName != null) {
                path = Paths.get(explicitFileName);
            }
        }
        if (path != null) {
            try {
                PrintStream stream = new PrintStream(Files.newOutputStream(path));
                if (streamName.equals("out")) {
                    System.setOut(new PrintStream(stream));
                } else {
                    System.setErr(new PrintStream(stream));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final Properties properties = new Properties(defaultProperties);

    static {
        loadProperties(properties, APL_INSTALLER_PROPERTIES, true);
        loadProperties(properties, APL_PROPERTIES, false);
    }

    public static Properties loadProperties(Properties properties, String propertiesFile, boolean isDefault) {
        try {
            // Load properties from location specified as command line parameter
            String configFile = System.getProperty(propertiesFile);
            if (configFile != null) {
                System.out.printf("Loading %s from %s\n", propertiesFile, configFile);
                try (InputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                    return properties;
                } catch (IOException e) {
                    throw new IllegalArgumentException(String.format("Error loading %s from %s", propertiesFile, configFile));
                }
            } else {
                try (InputStream is = ClassLoader.getSystemResourceAsStream(propertiesFile)) {
                    // When running apl.exe from a Windows installation we always have apl.properties in the classpath but this is not the apl properties file
                    // Therefore we first load it from the classpath and then look for the real apl.properties in the user folder.
                    if (is != null) {
                        System.out.printf("Loading %s from classpath\n", propertiesFile);
                        properties.load(is);
                        if (isDefault) {
                            return properties;
                        }
                    }
                    // load non-default properties files from the user folder
                    if (!dirProvider.isLoadPropertyFileFromUserDir()) {
                        return properties;
                    }
                    String homeDir = dirProvider.getUserHomeDir();
                    if (!Files.isReadable(Paths.get(homeDir))) {
                        System.out.printf("Creating dir %s\n", homeDir);
                        try {
                            Files.createDirectory(Paths.get(homeDir));
                        } catch(Exception e) {
                            if (!(e instanceof NoSuchFileException)) {
                                throw e;
                            }
                            // Fix for WinXP and 2003 which does have a roaming sub folder
                            Files.createDirectory(Paths.get(homeDir).getParent());
                            Files.createDirectory(Paths.get(homeDir));
                        }
                    }
                    Path confDir = Paths.get(homeDir, CONFIG_DIR);
                    if (!Files.isReadable(confDir)) {
                        System.out.printf("Creating dir %s\n", confDir);
                        Files.createDirectory(confDir);
                    }
                    Path propPath = Paths.get(confDir.toString()).resolve(Paths.get(propertiesFile));
                    if (Files.isReadable(propPath)) {
                        System.out.printf("Loading %s from dir %s\n", propertiesFile, confDir);
                        properties.load(Files.newInputStream(propPath));
                    } else {
                        System.out.printf("Creating property file %s\n", propPath);
                        Files.createFile(propPath);
                        Files.write(propPath, Convert.toBytes("# use this file for workstation specific " + propertiesFile));
                    }
                    return properties;
                } catch (IOException e) {
                    throw new IllegalArgumentException("Error loading " + propertiesFile, e);
                }
            }
        } catch(IllegalArgumentException e) {
            e.printStackTrace(); // make sure we log this exception
            throw e;
        }
    }

    // For using Apl.shutdown instead of System.exit
    static void removeShutdownHook() {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }

    public static File getLogDir() {
        return dirProvider.getLogFileDir();
    }

    private static void printCommandLineArguments() {
        try {
            List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            if (inputArguments != null && inputArguments.size() > 0) {
                System.out.println("Command line arguments");
            } else {
                return;
            }
            inputArguments.forEach(System.out::println);
        } catch (AccessControlException e) {
            System.out.println("Cannot read input arguments " + e.getMessage());
        }
    }

    public static int getIntProperty(String name) {
        return getIntProperty(name, 0);
    }

    public static int getIntProperty(String name, int defaultValue) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            LOG.info(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            LOG.info(name + " not defined or not numeric, using default value " + defaultValue);
            return defaultValue;
        }
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
        String value = properties.getProperty(name);
        if (value != null && ! "".equals(value)) {
            LOG.info(name + " = \"" + (doNotLog ? "{not logged}" : value) + "\"");
        } else {
            LOG.info(name + " not defined");
            value = defaultValue;
        }
        if (encoding == null || value == null) {
            return value;
        }
        try {
            return new String(value.getBytes("ISO-8859-1"), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
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

    public static boolean getBooleanProperty(String name) {
        return getBooleanProperty(name, false);
    }

    public static boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            LOG.info(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            LOG.info(name + " = \"false\"");
            return false;
        }
        LOG.info(name + " not defined, using default " + defaultValue);
        return defaultValue;
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
        Apl.time = time;
    }

    public static void main(String[] args) {
        try {
            shutdownHook = new Thread(Apl::shutdown);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            init();
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
            t.printStackTrace();
        }
    }

    public static void init(Properties customProperties) {
        properties.putAll(customProperties);
        init();
    }

    public static void init() {
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
        LOG.info(Apl.APPLICATION + " server " + VERSION + " stopped.");
        runtimeMode.shutdown();
        Apl.shutdown = true;
    }

    public static Chain getActiveChain() throws IOException {
        return chainIdService.getActiveChain();
    }


    private static class Init {

        private static volatile boolean initialized = false;

        static {
            try {

                long startTime = System.currentTimeMillis();
                chainIdService = new ChainIdServiceImpl(
                        (Apl.getStringProperty("apl.chainIdFilePath" , "chains.json")));
                Constants.init(getActiveChain());
                setSystemProperties();
                logSystemProperties();
                runtimeMode.init();
                Thread secureRandomInitThread = initSecureRandom();
                runtimeMode.updateAppStatus("Database initialization...");

                checkPorts();
                setServerStatus(ServerStatus.BEFORE_DATABASE, null);
                Db.init();
                ChainIdDbMigration.migrate();
                setServerStatus(ServerStatus.AFTER_DATABASE, null);
                Constants.updateToLatestConstants();
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
                initUpdater();
                DebugTrace.init();
                int timeMultiplier = (Constants.isTestnet() && Constants.isOffline) ? Math.max(Apl.getIntProperty("apl.timeMultiplier"), 1) : 1;
                ThreadPool.start(timeMultiplier);
                if (timeMultiplier > 1) {
                    setTime(new Time.FasterTime(Math.max(getEpochTime(), Apl.getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
                    LOG.info("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
                }
                try {
                    secureRandomInitThread.join(10000);
                }
                catch (InterruptedException ignore) {}
                testSecureRandom();
                long currentTime = System.currentTimeMillis();
                LOG.info("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
                String message = Apl.APPLICATION + " server " + VERSION + " started successfully.";
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
                if (isDesktopApplicationEnabled()) {
                    runtimeMode.updateAppStatus("Starting desktop application...");
                    launchDesktopApplication();
                }
                if (Constants.isTestnet()) {
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
            final int port = Constants.isTestnet() ?  Constants.TESTNET_API_PORT: Apl.getIntProperty("apl.apiServerPort");
            final int sslPort = Constants.isTestnet() ? TESTNET_API_SSLPORT : Apl.getIntProperty("apl.apiServerSSLPort");
            boolean enableSSL = Apl.getBooleanProperty("apl.apiSSL");
            int peerPort = -1;

            String myAddress = Convert.emptyToNull(Apl.getStringProperty("apl.myAddress", "").trim());
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
                peerPort = Constants.isTestnet() ? TESTNET_PEER_PORT : DEFAULT_PEER_PORT;
            }
            int peerServerPort = Apl.getIntProperty("apl.peerServerPort");

            Set<Integer> ports = new HashSet<>();
            ports.add(port);
            if (enableSSL) {
                ports.add(sslPort);
            }
            ports.add(peerPort);
            ports.add(Constants.isTestnet() ? TESTNET_PEER_PORT : peerServerPort);
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

    private static void setSystemProperties() {
      // Override system settings that the user has define in apl.properties file.
      String[] systemProperties = new String[] {
        "socksProxyHost",
        "socksProxyPort",
      };

      for (String propertyName : systemProperties) {
        String propertyValue;
        if ((propertyValue = getStringProperty(propertyName)) != null) {
          System.setProperty(propertyName, propertyValue);
        }
      }
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

    public static String getDbDir(String dbDir, UUID chainId) {
        return dirProvider.getDbDir(dbDir, chainId);
    }

    public static String getDbDir(String dbDir) {
        return dirProvider.getDbDir(dbDir, Constants.getChain().getChainId());
    }

    public static String getOldDbDir(String dbDir,  UUID chainId) {
        return dirProvider
                .getDbDir(dbDir, chainId)
                .replace(String.valueOf(chainId) + File.separator, "");
    }

    public static String getOldDbDir(String dbDir) {
        return getOldDbDir(dbDir, Constants.getChain().getChainId());
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

    public static boolean isDesktopApplicationEnabled() {
        return RuntimeEnvironment.isDesktopApplicationEnabled() && Apl.getBooleanProperty("apl.launchDesktopApplication");
    }

    private static void launchDesktopApplication() {
        runtimeMode.launchDesktopApplication();
    }

    private Apl() {} // never

    private static void initUpdater() {
        if (!getBooleanProperty("apl.allowUpdates", false)) {
            return;
        }
        try {
            Class<?> aClass = Class.forName("com.apollocurrency.aplwallet.apl.updater.UpdaterCore");
            //force load lazy updater instance
            aClass.getMethod("getInstance").invoke(null);

        }
        catch (Exception e) {
            LOG.error("Cannot load Updater!", e);
        }
    }
}
