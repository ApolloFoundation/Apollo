/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.quarkus;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.migrator.MigratorUtil;
import com.apollocurrency.aplwallet.apl.exec.CustomDirLocations;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.updater.core.UpdaterCoreImpl;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.env.EnvironmentVariables;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeParams;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.PredefinedDirLocations;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class NodeApplicationStart {
    //    System properties to load by PropertiesConfigLoader
    public static final String PID_FILE = "apl.pid";
    public static final String CMD_FILE = "apl.cmdline";
    public static final String APP_FILE = "apl.app";

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
    //We have dir provider configured in logback.xml so should init log later
    private static Logger log;
    private PropertiesHolder propertiesHolder;
    private static AplCoreRuntime aplCoreRuntime;

//    @Inject
    private ApolloCommandLine args;
//
/*
    @Inject
    private EnvironmentVariables envVars;
    @Inject
    ConfigDirProviderFactory configDirProviderFactory;
    @Inject
    private EnvConfig envConfig;
    @Inject
    private ConfigDirProvider configDirProvider;
//    @Inject
    private TaskDispatchManager taskDispatchManager;
*/
    private Properties props;
    private Map<UUID, Chain> chains;

//    @Inject
    public NodeApplicationStart(/*ApolloCommandLine args*/) {
//        this.args = args;

//        RuntimeEnvironment.getInstance().setMain(Apollo.class);

// We do not need it yet. this call creates unwanted error messages
//        if(RuntimeEnvironment.getInstance().isAdmin()){
//            System.out.println("==== RUNNING WITH ADMIN/ROOT PRIVILEGES! ====");
//        }
        System.setProperty("apl.runtime.mode", args.serviceMode ? "service" : "user");

        System.setProperty("javax.net.ssl.trustStore", "cacerts");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");

/*
        String configDir = StringUtils.isBlank(args.configDir) ? args.configDir : args.configDir;

        ConfigDirProviderFactory.setup(args.serviceMode, Constants.APPLICATION_DIR_NAME,
            args.netIdx, args.chainId, configDir);

        ConfigDirProvider configDirProvider = ConfigDirProviderFactory.getConfigDirProvider();

// Well, we can not resolve chainID for given parameters and therefor can not read configs. We have to exit program
        if (configDirProvider.getChainId() == null) {
            System.exit(PosixExitCodes.EX_CONFIG.exitCode());
        }

        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
            configDirProvider,
            args.ignoreResources,
            configDir,
            Constants.APPLICATION_DIR_NAME + ".properties",
            SYSTEM_PROPERTY_NAMES);

        //cheat classloader to get access to package resources
//load configuration files
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(
            configDirProvider,
            configDir,
            args.ignoreResources
        );
// init application data dir provider

        this.chains = chainsConfigLoader.load();
        UUID chainId = ChainUtils.getActiveChain(chains).getChainId();
        this.props = propertiesLoader.load();
//over-write config options from command line if set
        if (args.noShardImport != null) {
            props.setProperty("apl.noshardimport", "" + args.noShardImport);
        }
        if (args.noShardCreate != null) {
            props.setProperty("apl.noshardcreate", "" + args.noShardCreate);
        }

        CustomDirLocations customDirLocations = new CustomDirLocations(getCustomDbPath(chainId, props),
            props.getProperty(CustomDirLocations.KEYSTORE_DIR_PROPERTY_NAME));
        DirProviderFactory.setup(args.serviceMode, chainId, Constants.APPLICATION_DIR_NAME,
            merge(args, this.envVars, customDirLocations));
        dirProvider = DirProviderFactory.getProvider();
        RuntimeEnvironment.getInstance().setDirProvider(dirProvider);
        //init logging
        logDirPath = dirProvider.getLogsDir().toAbsolutePath();
        log = LoggerFactory.getLogger(Apollo.class);
        if (args.debug != CommandLineArgument.DEFAULT_DEBUG_LEVEL) {
            setLogLevel(args.debug);
        }
*/

//check webUI
        System.out.println("=== Bin directory is: " + DirProvider.getBinDir().toAbsolutePath());
        /* at the moment we do it in build time

        Future<Boolean> unzipRes;
        WebUiExtractor we = new WebUiExtractor(dirProvider);
        ExecutorService execService = Executors.newFixedThreadPool(1);
        unzipRes = execService.submit(we);
         */
    }

    public void run() {
        runtimeMode = RuntimeEnvironment.getInstance().getRuntimeMode();
/*
        runtimeMode.init(); // instance is NOT PROXIED by CDI !!

        //save command line params and PID
        if (!saveStartParams(args.pidFile, configDirProvider)) {
            System.exit(PosixExitCodes.EX_CANTCREAT.exitCode());
        }

        // init config holders
        this.propertiesHolder = new PropertiesHolder();
        this.propertiesHolder.init(props);
        if (log != null) {
            log.trace("{}", this.propertiesHolder.dumpAllProperties()); // dumping all properties
        }
        this.taskDispatchManager = new TaskDispatchManager(propertiesHolder);
        ChainsConfigHolder chainsConfigHolder = new ChainsConfigHolder();
        chainsConfigHolder.setChains(chains);
*/
/*
        BlockchainConfigUpdater blockchainConfigUpdater = CDI.current().select(BlockchainConfigUpdater.class).get();
        blockchainConfigUpdater.updateChain(chainsConfigHolder.getActiveChain(), this.propertiesHolder);
        dirProvider = CDI.current().select(DirProvider.class).get();
        // init secureStorageService instance via CDI for 'ShutdownHook' constructor below
        SecureStorageService secureStorageService = CDI.current().select(SecureStorageService.class).get();
        aplCoreRuntime = CDI.current().select(AplCoreRuntime.class).get();
        BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        aplCoreRuntime.init(runtimeMode, blockchainConfig, this.propertiesHolder, this.taskDispatchManager);
        Convert2.init(blockchainConfig);

        try {
            // updated shutdown hook explicitly created with instances
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(aplCoreRuntime));
            aplCoreRuntime.addCoreAndInit();
            this.initUpdater(args.updateAttachmentFile, args.debugUpdater);
            if (args.startMint) {
                aplCoreRuntime.startMinter();
            }
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
            t.printStackTrace();
        }
*/
    }

    private static void setLogLevel(int logLevel) {
        // let's SET LEVEL EXPLICITLY only when it was passed via command line params
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

    public boolean saveStartParams(String pidPath, ConfigDirProvider configDirProvider) {
        boolean res = true;
        String cmdline = "";
//        for (CommandLine.ParseResult s : this.args) {
//            cmdline = cmdline + s + " ";
//            log.debug("s = {}", s);
//        }
//        log.debug("cmdline = {}", cmdline);
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

    public static PredefinedDirLocations merge(ApolloCommandLine args, EnvironmentVariables vars, CustomDirLocations customDirLocations) {
        return new PredefinedDirLocations(
            customDirLocations.getDbDir().isEmpty() ? StringUtils.isBlank(args.dbDir) ?
                vars.dbDir : args.dbDir : customDirLocations.getDbDir().get(),
            StringUtils.isBlank(args.logDir) ? vars.logDir : args.logDir,
            customDirLocations.getKeystoreDir().isEmpty() ? StringUtils.isBlank(args.vaultKeystoreDir) ?
                vars.vaultKeystoreDir : args.vaultKeystoreDir : customDirLocations.getKeystoreDir().get(),
            StringUtils.isBlank(args.pidFile) ? vars.pidFile : args.pidFile,
            StringUtils.isBlank(args.twoFactorAuthDir) ? vars.twoFactorAuthDir : args.twoFactorAuthDir,
            StringUtils.isBlank(args.dataExportDir) ? vars.dataExportDir : args.dataExportDir,
            StringUtils.isBlank(args.dexKeystoreDir) ? vars.dexKeystoreDir : args.dexKeystoreDir
        );
    }


    public static void shutdownWeldContainer() {
        try {
//            container.shutdown();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static String getCustomDbPath(UUID chainId, Properties properties) { //maybe better to set dbUrl or add to dirProvider
        String customDbDir = properties.getProperty(CustomDirLocations.DB_DIR_PROPERTY_NAME);
        if (customDbDir != null) {
            Path legacyHomeDir = MigratorUtil.getLegacyHomeDir();
            Path customDbPath = legacyHomeDir.resolve(customDbDir).resolve(chainId.toString().substring(0, 6)).normalize();
            System.out.println("Using custom db path " + customDbPath.toAbsolutePath().toString());
            return customDbPath.toAbsolutePath().toString();
        }
        return null;
    }

    private void initUpdater(String attachmentFilePath, boolean debug) {
        if (!propertiesHolder.getBooleanProperty("apl.allowUpdates", false)) {
            return;
        }
        UpdaterCore updaterCore = CDI.current().select(UpdaterCoreImpl.class).get();
        updaterCore.init(attachmentFilePath, debug);
    }

}
