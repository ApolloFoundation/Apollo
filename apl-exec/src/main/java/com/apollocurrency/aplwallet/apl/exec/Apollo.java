/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exec;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.apollocurrency.aplwallet.apl.conf.ConfPlaceholder;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.migrator.MigratorUtil;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
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
    private static AplContainer container;
    private static AplCoreRuntime aplCoreRuntime;
    private PropertiesHolder propertiesHolder;
    private TaskDispatchManager taskDispatchManager;

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
        Path hp = Paths.get(configDirProvider.getUserConfigDirectory()).getParent();
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

    /**
     * @param argv the command line arguments
     */
    public static void main(String[] argv) {
        System.out.println("Initializing Apollo");
        Apollo app = new Apollo();

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
        if (args.help) {
            jc.usage();
            System.exit(PosixExitCodes.OK.exitCode());
        }

        RuntimeEnvironment.getInstance().setMain(Apollo.class);

// We do not need it yet. this call creates unwanted error messages
//        if(RuntimeEnvironment.getInstance().isAdmin()){
//            System.out.println("==== RUNNING WITH ADMIN/ROOT PRIVILEGES! ====");
//        }
        System.setProperty("apl.runtime.mode", args.serviceMode ? "service" : "user");

        System.setProperty("javax.net.ssl.trustStore", "cacerts");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");

//cheat classloader to get access to package resources
        ConfPlaceholder ph = new ConfPlaceholder();
//load configuration files
        EnvironmentVariables envVars = new EnvironmentVariables(Constants.APPLICATION_DIR_NAME);
        ConfigDirProviderFactory.setup(args.serviceMode, Constants.APPLICATION_DIR_NAME, args.netIdx);
        ConfigDirProvider configDirProvider = ConfigDirProviderFactory.getConfigDirProvider();

        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
            configDirProvider,
            args.isResourceIgnored(),
            StringUtils.isBlank(args.configDir) ? envVars.configDir : args.configDir,
            Constants.APPLICATION_DIR_NAME + ".properties",
            SYSTEM_PROPERTY_NAMES);

        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(
            configDirProvider,
            StringUtils.isBlank(args.configDir) ? envVars.configDir : args.configDir,
            args.isResourceIgnored()
        );
// init application data dir provider

        Map<UUID, Chain> chains = chainsConfigLoader.load();
        UUID chainId = ChainUtils.getActiveChain(chains).getChainId();
        Properties props = propertiesLoader.load();
//over-write config options from command line if set
        if (args.noShardImport != null) {
            props.setProperty("apl.noshardimport", "" + args.noShardImport);
        }
        if (args.noShardCreate != null) {
            props.setProperty("apl.noshardcreate", "" + args.noShardCreate);
        }

        CustomDirLocations customDirLocations = new CustomDirLocations(getCustomDbPath(chainId, props), props.getProperty(CustomDirLocations.KEYSTORE_DIR_PROPERTY_NAME));
        DirProviderFactory.setup(args.serviceMode, chainId, Constants.APPLICATION_DIR_NAME, merge(args, envVars, customDirLocations));
        dirProvider = DirProviderFactory.getProvider();
        RuntimeEnvironment.getInstance().setDirProvider(dirProvider);
        //init logging
        logDirPath = dirProvider.getLogsDir().toAbsolutePath();
        log = LoggerFactory.getLogger(Apollo.class);
        if (args.debug != CmdLineArgs.DEFAULT_DEBUG_LEVEL) {
            setLogLevel(args.debug);
        }

//check webUI
        System.out.println("=== Bin directory is: " + DirProvider.getBinDir().toAbsolutePath());
        /* at the moment we do it in build time

        Future<Boolean> unzipRes;
        WebUiExtractor we = new WebUiExtractor(dirProvider);
        ExecutorService execService = Executors.newFixedThreadPool(1);
        unzipRes = execService.submit(we);
         */

        runtimeMode = RuntimeEnvironment.getInstance().getRuntimeMode();
        runtimeMode.init(); // instance is NOT PROXIED by CDI !!

        //save command line params and PID
        if (!saveStartParams(argv, args.pidFile, configDirProvider)) {
            System.exit(PosixExitCodes.EX_CANTCREAT.exitCode());
        }

        //Configure CDI Container builder
        AplContainerBuilder aplContainerBuilder = AplContainer.builder().containerId("MAIN-APL-CDI")
            // do not use recursive scan because it violates the restriction to
            // deploy one bean for all deployment archives
            // Recursive scan will trigger base synthetic archive to load JdbiTransactionalInterceptor, which was already loaded by apl-core archive
            // See https://docs.jboss.org/cdi/spec/2.0.EDR2/cdi-spec.html#se_bootstrap for more details
            // we already have it in beans.xml in core
            .annotatedDiscoveryMode();
        //TODO:  turn it on periodically in development process to check CDI errors
        // Enable for development only, see http://weld.cdi-spec.org/news/2015/11/10/weld-probe-jmx/
        // run with ./bin/apl-run-jmx.sh
        //.devMode()
        if (args.disableWeldConcurrentDeployment) {
            //It's very helpful when the application is stuck during the Weld Container building.
            log.info("The concurrent deployment of Weld container is disabled.");
            aplContainerBuilder.disableConcurrentDeployment();
        }

        //init CDI container
        container = aplContainerBuilder.build();

        log.debug("Weld CDI container build done");
        // init config holders
        app.propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
        app.propertiesHolder.init(props);
        if (log != null) log.trace("{}", app.propertiesHolder.dumpAllProperties()); // dumping all properties

        app.taskDispatchManager = CDI.current().select(TaskDispatchManager.class).get();
        ChainsConfigHolder chainsConfigHolder = CDI.current().select(ChainsConfigHolder.class).get();
        chainsConfigHolder.setChains(chains);
        BlockchainConfigUpdater blockchainConfigUpdater = CDI.current().select(BlockchainConfigUpdater.class).get();
        blockchainConfigUpdater.updateChain(chainsConfigHolder.getActiveChain(), app.propertiesHolder);
        dirProvider = CDI.current().select(DirProvider.class).get();
        // init secureStorageService instance via CDI for 'ShutdownHook' constructor below
        SecureStorageService secureStorageService = CDI.current().select(SecureStorageService.class).get();
        aplCoreRuntime = CDI.current().select(AplCoreRuntime.class).get();
        BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        aplCoreRuntime.init(runtimeMode, blockchainConfig, app.propertiesHolder, app.taskDispatchManager);
        Convert2.init(blockchainConfig);

        try {
            // updated shutdown hook explicitly created with instances
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(aplCoreRuntime));
            aplCoreRuntime.addCoreAndInit();
            app.initUpdater(args.updateAttachmentFile, args.debugUpdater);
            if (args.startMint) {
                aplCoreRuntime.startMinter();
            }
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
