/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exec;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.apollocurrency.aplwallet.apl.conf.ConfPlaceholder;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.db.DbConfig;
import com.apollocurrency.aplwallet.apl.core.service.appdata.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.utils.LegacyDbUtil;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.updater.core.UpdaterCoreImpl;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.cdi.AplContainer;
import com.apollocurrency.aplwallet.apl.util.cdi.AplContainerBuilder;
import com.apollocurrency.aplwallet.apl.util.env.EnvironmentVariables;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeParams;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainUtils;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.PredefinedDirLocations;
import com.apollocurrency.aplwallet.apl.util.injectable.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Main Apollo startup class
 *
 * @author alukin@gmail.com
 */
// @Singleton
public class Apollo {

    //    System properties to load by PropertiesConfigLoader
    public static final String PID_FILE = "apl.pid";
    public static final String CMD_FILE = "apl.cmdline";
    public static final String APP_FILE = "apl.app";
    public static final String APOLLO_MARIADB_INSTALL_DIR="apollo-mariadb";
    private static final List<String> SYSTEM_PROPERTY_NAMES = Arrays.asList(
        "socksProxyHost",
        "socksProxyPort",
        "apl.enablePeerUPnP",
        "apl.enableAPIUPnP"
    );
    private final static String[] VALID_LOG_LEVELS = {"ERROR", "WARN", "INFO", "DEBUG", "TRACE"};
    //This variable is used in LogDirPropertyDefiner configured in logback.xml
    public static Path logDirPath = Paths.get("");
    public static RuntimeMode runtimeMode;
    public static DirProvider dirProvider;
    //initially we do not have control over  MariaDB process, it could be startted externally or system-wide
    public static MariaDbProcess mariaDbProcess = null;
    //We have dir provider configured in logback.xml so should init log later
    private static Logger log;
    private static AplContainer container;
    private static AplCoreRuntime aplCoreRuntime;

    private static void setLogLevel(int logLevel) {
        // let's SET LEVEL EXPLOCITLY only when it was passed via command line params
        String packageName = "com.apollocurrency.aplwallet.apl";
        if (logLevel >= VALID_LOG_LEVELS.length - 1) {
            logLevel = VALID_LOG_LEVELS.length - 1;
        }
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(packageName);
        System.out.println(packageName + " current logger level: " + logger.getLevel()
            + " New level: " + VALID_LOG_LEVELS[logLevel]);

        logger.setLevel(Level.toLevel(VALID_LOG_LEVELS[logLevel]));
        // otherwise we want to load usual logback.xml settings
    }

    public static boolean saveStartParams(String[] argv, String pidPath, ConfigDirProvider configDirProvider) {
        boolean res = true;
        String cmdline = "";
        for (String s : argv) {
            cmdline = cmdline + s + " ";
        }
        Path hp = Paths.get(configDirProvider.getUserConfigLocation());
        String home = hp.toString() + File.separator;
        File dir = new File(home);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String path = pidPath.isEmpty() ? home + PID_FILE : pidPath;
        try (PrintWriter out = new PrintWriter(path)) {
            out.println(RuntimeParams.getProcessId());
        } catch (FileNotFoundException ex) {
            System.err.println("Can not write PID to: " + path);
            res = false;
        }
        path = home + CMD_FILE;
        try (PrintWriter out = new PrintWriter(path)) {
            out.println(cmdline);
        } catch (FileNotFoundException ex) {
            System.err.println("Can not write command line args file to: " + path);
            res = false;
        }
        path = home + APP_FILE;
        try (PrintWriter out = new PrintWriter(home + APP_FILE)) {
            out.println(DirProvider.getBinDir());
        } catch (FileNotFoundException ex) {
            System.err.println("Can not write Apollo start path file to: " + path);
            res = false;
        }
        return res;
    }

    public static PredefinedDirLocations merge(CmdLineArgs args, EnvironmentVariables vars, CustomDirLocations customDirLocations) {
        return new PredefinedDirLocations(
            customDirLocations.getDbDir().isEmpty() ? StringUtils.isBlank(args.dbDir) ? vars.dbDir : args.dbDir : customDirLocations.getDbDir().get(),
            StringUtils.isBlank(args.logDir) ? vars.logDir : args.logDir,
            customDirLocations.getKeystoreDir().isEmpty() ? StringUtils.isBlank(args.vaultKeystoreDir) ? vars.vaultKeystoreDir : args.vaultKeystoreDir : customDirLocations.getKeystoreDir().get(),
            StringUtils.isBlank(args.pidFile) ? vars.pidFile : args.pidFile,
            StringUtils.isBlank(args.twoFactorAuthDir) ? vars.twoFactorAuthDir : args.twoFactorAuthDir,
            StringUtils.isBlank(args.dataExportDir) ? vars.dataExportDir : args.dataExportDir,
            StringUtils.isBlank(args.dexKeystoreDir) ? vars.dexKeystoreDir : args.dexKeystoreDir
        );
    }

    public static void setSystemProperties(CmdLineArgs args){
        System.setProperty("apl.runtime.mode", args.serviceMode ? "service" : "user");
        System.setProperty("javax.net.ssl.trustStore", "cacerts");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }

    private static String getCustomDbPath(UUID chainId, Properties properties) { //maybe better to set dbUrl or add to dirProvider
        String customDbDir = properties.getProperty(CustomDirLocations.DB_DIR_PROPERTY_NAME);
        if (customDbDir != null) {
            Path legacyHomeDir = LegacyDbUtil.getLegacyHomeDir();
            Path customDbPath = legacyHomeDir.resolve(customDbDir).resolve(chainId.toString().substring(0, 6)).normalize();
            System.out.println("Using custom db path " + customDbPath.toAbsolutePath().toString());
            return customDbPath.toAbsolutePath().toString();
        }
        return null;
    }

    private void initUpdater(String attachmentFilePath, boolean debug, PropertiesHolder propertiesHolder) {
        if (!propertiesHolder.getBooleanProperty("apl.allowUpdates", false)) {
            return;
        }
        UpdaterCore updaterCore = CDI.current().select(UpdaterCoreImpl.class).get();
        updaterCore.init(attachmentFilePath, debug);
    }

    private static boolean checkDbWithJDBC(DbConfig conf){
        boolean res = true;
        DbProperties dbConfig = conf.getDbConfig();
        String dbURL = dbConfig.formatJdbcUrlString(true);
        Connection conn;
        try {
            conn = DriverManager.getConnection(dbURL);
            if(!conn.isValid(1)){
                res = false;
            }
        } catch (SQLException ex) {
            res = false;
        }

        return res;
    }

    private static boolean checkOrRunDatabaseServer(DbConfig conf) {
        boolean res = checkDbWithJDBC(conf);
        //if we have connected to database URL from config, wha have nothing to do
        if(!res){
            // if we can not connect to databse, we'll try start it
            // from Apollo package. If it is first start, data base data dir
            // will be initialized
            Path dbDataDir = dirProvider.getDbDir();
            Path dbInstalPath = DirProvider.getBinDir().getParent().resolve(APOLLO_MARIADB_INSTALL_DIR);
            mariaDbProcess = new MariaDbProcess(conf,dbInstalPath,dbDataDir);
            res = mariaDbProcess.startAndWaitWhenReady();
        }
        return res;
    }

    /**
     * @param argv the command line arguments
     */
    public static void main(String[] argv) {
        System.out.println("Initializing Apollo");
        Apollo app = new Apollo();

//parse command line first

        CmdLineArgs args = new CmdLineArgs();
        JCommander jc = JCommander.newBuilder()
            .addObject(args)
            .build();
        jc.setProgramName(Constants.APPLICATION);
        try {
            jc.parse(argv);
        } catch (RuntimeException ex) {
            System.err.println("Error parsing command line arguments.");
            System.err.println(ex.getMessage());
            jc.usage();
            System.exit(PosixExitCodes.EX_USAGE.exitCode());
        }
        if (args.getNetIdx() >= 0 && !args.chainId.isEmpty()) {
            System.err.println("--chainId, --testnet and --net parameters are incompatible, please specify only one");
            System.exit(PosixExitCodes.EX_USAGE.exitCode());
        }
        if (args.help) {
            jc.usage();
            System.exit(PosixExitCodes.OK.exitCode());
        }


//set main application class to runtime
        RuntimeEnvironment.getInstance().setMain(Apollo.class);
//set some important system properties
        setSystemProperties(args);
//cheat classloader to get access to "conf" package resources
        ConfPlaceholder ph = new ConfPlaceholder();

//--------------- config locading section -------------------------------------

//load configuration files
        EnvironmentVariables envVars = new EnvironmentVariables(Constants.APPLICATION_DIR_NAME);

        String configDir = StringUtils.isBlank(args.configDir) ? envVars.configDir : args.configDir;
        ConfigDirProviderFactory.setup(args.serviceMode, Constants.APPLICATION_DIR_NAME, args.netIdx, args.chainId, configDir);
        ConfigDirProvider configDirProvider = ConfigDirProviderFactory.getConfigDirProvider();
//save command line params and PID
        if (!saveStartParams(argv, args.pidFile, configDirProvider)) {
            System.exit(PosixExitCodes.EX_CANTCREAT.exitCode());
        }

// Well, we can not resolve chainID we have to run with from given parameters
// and therefore can not read configs. We have to exit program
        if (configDirProvider.getChainId() == null) {
            System.err.println("ERROR: Can not resolve chain ID to run with from given command line arguments of configs!");
            System.exit(PosixExitCodes.EX_CONFIG.exitCode());
        }

//load configuration files
        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
            configDirProvider,
            args.isResourceIgnored(),
            configDir,
            Constants.APPLICATION_DIR_NAME + ".properties",
            SYSTEM_PROPERTY_NAMES);

// load everuthing into applicationProperies. This is the place where all configuration
// is collected from configs, command line and environment variables
        Properties applicationProperties = propertiesLoader.load();

        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(
            configDirProvider,
            configDir,
            args.isResourceIgnored()
        );

// init chains configurations by loading chains.json file
        Map<UUID, Chain> chains = chainsConfigLoader.load();
        UUID chainId = ChainUtils.getActiveChain(chains).getChainId();

//over-write config options from command line if set

        if (args.noShardImport) {
            applicationProperties.setProperty("apl.noshardimport", "" + args.noShardImport);
        }
        if (args.noShardCreate) {
            applicationProperties.setProperty("apl.noshardcreate", "" + args.noShardCreate);
        }
//TODO: check this piece of art
        CustomDirLocations customDirLocations = new CustomDirLocations(
                getCustomDbPath(chainId, applicationProperties),
                applicationProperties.getProperty(CustomDirLocations.KEYSTORE_DIR_PROPERTY_NAME)
        );

        DirProviderFactory.setup(args.serviceMode,
                chainId,
                Constants.APPLICATION_DIR_NAME,
                merge(args, envVars, customDirLocations)
        );

        dirProvider = DirProviderFactory.getProvider();
        RuntimeEnvironment.getInstance().setDirProvider(dirProvider);

//init logging
        logDirPath = dirProvider.getLogsDir().toAbsolutePath();
        log = LoggerFactory.getLogger(Apollo.class);
        if (args.debug != CmdLineArgs.DEFAULT_DEBUG_LEVEL) {
            setLogLevel(args.debug);
        }

        System.out.println("=== INFO: Bin directory of apollo-blockchain is: " + DirProvider.getBinDir().toAbsolutePath()+" ===");

// runtimeMode could be user or service. It is also different for Unix and Windows
        runtimeMode = RuntimeEnvironment.getInstance().getRuntimeMode();
        runtimeMode.init(); // instance is NOT PROXIED by CDI !!

// check running or run data base server process.

        DbConfig dbConfig = new DbConfig(new PropertiesHolder(applicationProperties), new ChainsConfigHolder(chains));
        if(!checkOrRunDatabaseServer(dbConfig)){
            System.err.println(" ERROR! MariaDB process is not running and can not be started from Apollo!");
            System.err.println(" Please install apollo-mariadb package at the same directory level as apollo-blockchain package.");
            System.exit(PosixExitCodes.EX_SOFTWARE.exitCode());
        }

//-------------- now bring CDI container up! -------------------------------------

//Configure CDI Container builder and start CDI container. From now all things must go CDI way
        AplContainerBuilder aplContainerBuilder = AplContainer.builder().containerId("MAIN-APL-CDI")
            // do not use recursive scan because it violates the restriction to
            // deploy one bean for all deployment archives
            // Recursive scan will trigger base synthetic archive to load JdbiTransactionalInterceptor, which was already loaded by apl-core archive
            // See https://docs.jboss.org/cdi/spec/2.0.EDR2/cdi-spec.html#se_bootstrap for more details
            // we already have it in beans.xml in core
            .annotatedDiscoveryMode();

        //!!!!!!!!!!!!!!
        //TODO:  turn it on periodically in development process to check CDI errors
        // Enable for development only, see http://weld.cdi-spec.org/news/2015/11/10/weld-probe-jmx/
        // run with ./bin/apl-run-jmx.sh
        //
        // aplContainerBuilder.devMode();
        //
        //!!!!!!!!!!!!!!!

        if (args.disableWeldConcurrentDeployment) {
            //It's very helpful when the application is stuck during the Weld Container building.
            log.info("The concurrent deployment of Weld container is disabled.");
            aplContainerBuilder.disableConcurrentDeployment();
        }

        //init CDI container
        container = aplContainerBuilder.build();

        log.debug("Weld CDI container build done");

// ------------------- NOW CDI is up and running, we have feed our configs to beans

//aplCoreRuntime is the producer for all config holders, initing it with configs

        aplCoreRuntime = CDI.current().select(AplCoreRuntime.class).get();


        aplCoreRuntime.init(runtimeMode, dirProvider, applicationProperties, chains);


        BlockchainConfigUpdater blockchainConfigUpdater = CDI.current().select(BlockchainConfigUpdater.class).get();
        blockchainConfigUpdater.updateChain(aplCoreRuntime.getChainsConfigHolder().getActiveChain(), aplCoreRuntime.getPropertieHolder());


        // init secureStorageService instance via CDI for 'ShutdownHook' constructor below
        SecureStorageService secureStorageService = CDI.current().select(SecureStorageService.class).get();
        aplCoreRuntime = CDI.current().select(AplCoreRuntime.class).get();
        BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();

        if (log != null) {
            log.trace("{}",aplCoreRuntime.getPropertieHolder().dumpAllProperties()); // dumping all properties
        }

        try {
            // updated shutdown hook explicitly created with instances
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(aplCoreRuntime));
            aplCoreRuntime.addCoreAndInit();
            app.initUpdater(args.updateAttachmentFile, args.debugUpdater, aplCoreRuntime.getPropertieHolder());
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
            t.printStackTrace();
        }
    }


    public static void shutdownWeldContainer() {
        try {
            container.shutdown();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }


}
