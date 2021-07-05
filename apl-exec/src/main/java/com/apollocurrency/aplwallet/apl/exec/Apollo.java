/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exec;

import static io.firstbridge.kms.persistence.storage.repository.KmsAccountRepository.KMS_SCHEMA_NAME;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.apollocurrency.aplwallet.apl.conf.ConfPlaceholder;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.db.DbConfig;
import com.apollocurrency.aplwallet.apl.core.kms.config.GrpcHostConfigImpl;
import com.apollocurrency.aplwallet.apl.core.kms.config.RemoteKmsConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.utils.LegacyDbUtil;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.updater.core.UpdaterCoreImpl;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.cdi.AplContainer;
import com.apollocurrency.aplwallet.apl.util.cdi.AplContainerBuilder;
import com.apollocurrency.aplwallet.apl.util.db.MariaDbProcess;
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
import io.firstbridge.kms.client.grpx.GrpcClient;
import io.firstbridge.kms.client.grpx.config.GrpcHostConfig;
import io.firstbridge.kms.security.KmsMainConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Main Apollo startup class
 *
 * @author alukin@gmail.com
 */
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

    /**
     * KMS server validation works with IPv4 (1.1.1.1:123), IPv6 ([::0]:123) and host names (my.host.com:123)
     * @param args args with non empty 'kms' field to validate
     * @return Optional InetSocketAddress value or EMPTY
     */
    private static Optional<InetSocketAddress> validateKmsUrl(CmdLineArgs args, KmsMainConfig kmsMainConfig) {
        String ipUrlAsString = args.kms; // KMS standalone server has to be validated
        if (ipUrlAsString.strip().isBlank()) return Optional.empty();
        if (!ipUrlAsString.contains("://")) {
            ipUrlAsString = "dns://" + ipUrlAsString;
        }
        try {
            // WORKAROUND: add any scheme to make the resulting URI valid.
            URI uri = new URI(ipUrlAsString); // may throw URISyntaxException
            String host = uri.getHost();
            ((RemoteKmsConfigImpl)kmsMainConfig.getRemoteKmsConfig()).setAddress(host);

            if (uri.getHost() == null) {
                System.err.println("Can not assign KMS, URI must have host and port parts: " + args.kms);
                return Optional.empty();
            }
            // here, additional checks can be performed,
            // such as presence of path, query, fragment, ...
            // validation succeeded
            return Optional.of(new InetSocketAddress (
                kmsMainConfig.getRemoteKmsConfig().getAddress(),
                kmsMainConfig.getRemoteKmsConfig().getGrpcPort()));

        } catch (URISyntaxException ex) {
            // validation failed
            System.err.println("KMS ip/host is not valid: " + args.kms);
        }
        return Optional.empty();
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

// load everything into applicationProperties. This is the place where all configuration
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
        DbConfig dbConfig = CDI.current().select(DbConfig.class).get();// create proxied cdi component for later use
        aplCoreRuntime.init(runtimeMode, dirProvider, applicationProperties, chains, dbConfig);

        BlockchainConfigUpdater blockchainConfigUpdater = CDI.current().select(BlockchainConfigUpdater.class).get();
        blockchainConfigUpdater.updateChain(aplCoreRuntime.getChainsConfigHolder().getActiveChain(), aplCoreRuntime.getPropertiesHolder());

        // init secureStorageService instance via CDI for 'ShutdownHook' constructor below
        SecureStorageService secureStorageService = CDI.current().select(SecureStorageService.class).get();
        aplCoreRuntime = CDI.current().select(AplCoreRuntime.class).get();
        BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();

        KmsMainConfig kmsMainConfig = CDI.current().select(KmsMainConfig.class).get(); // KMS
        Optional<InetSocketAddress> kmsUrl = validateKmsUrl(args, kmsMainConfig);
        if (kmsUrl.isEmpty()) {
            // KMS embedded mode
            dbConfig.setKmsSchemaName(KMS_SCHEMA_NAME);
            log.debug("KMS is in 'embedded mode' in db schema = {}...", KMS_SCHEMA_NAME);
        } else {
            // KMS remote server mode via gRPC client
            String hostString = kmsUrl.get().toString();
            log.debug("Checking if KMS server connection is healthy by url = {}...", hostString);
            GrpcHostConfig grpcHostConfig = new GrpcHostConfigImpl(
                kmsMainConfig.getRemoteKmsConfig().getAddress(),
                kmsMainConfig.getRemoteKmsConfig().getGrpcPort()
            );
            GrpcClient grpcClient = new GrpcClient(grpcHostConfig);
            // check KMS server connectivity
            boolean healthy = grpcClient.isHealthy();
            log.debug("KMS server connection is {} by gRPC url = {}...", healthy ? "HEALTHY !" : "==> BROKEN !", hostString);
            grpcClient.shutDown();
            if (healthy) {
                ((RemoteKmsConfigImpl)kmsMainConfig.getRemoteKmsConfig()).setRemoteServerModeOn(true);
//                ((RemoteKmsConfigImpl)kmsMainConfig.getRemoteKmsConfig()).setAddress(hostString);
            }
        }

        if (log != null) {
            log.trace("{}",aplCoreRuntime.getPropertiesHolder().dumpAllProperties()); // dumping all properties
        }

        try {
            // updated shutdown hook explicitly created with instances
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(aplCoreRuntime));
            aplCoreRuntime.addCoreAndInit();
            app.initUpdater(args.updateAttachmentFile, args.debugUpdater, aplCoreRuntime.getPropertiesHolder());
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
