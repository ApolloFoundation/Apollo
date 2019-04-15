package com.apollocurrency.aplwallet.apl.exec;

import com.apollocurrency.aplwallet.api.dto.Account;
import com.apollocurrency.aplwallet.apl.core.app.AplCore;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.core.migrator.MigratorUtil;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiTransactionalInterceptor;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextTrigger;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.ServerInfoEndpoint;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.updater.core.Updater;
import com.apollocurrency.aplwallet.apl.updater.core.UpdaterCoreImpl;
import com.apollocurrency.aplwallet.apl.util.AppStatus;
import com.apollocurrency.aplwallet.apl.util.AppStatusUpdater;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.cdi.AplContainer;
import com.apollocurrency.aplwallet.apl.util.env.EnvironmentVariables;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainUtils;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.PredefinedDirLocations;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.enterprise.inject.spi.CDI;

/**
 * Main Apollo startup class
 *
 * @author alukin@gmail.com
 */
public class Apollo {
//    System properties to load by PropertiesConfigLoader
    private static final List<String> SYSTEM_PROPERTY_NAMES = Arrays.asList(
            "socksProxyHost",
            "socksProxyPort",
            "apl.enablePeerUPnP",
            "apl.enableAPIUPnP"
    );

    //This variable is used in LogDirPropertyDefiner configured in logback.xml
    public static Path logDirPath = Paths.get("");
    //We have dir provider configured in logback.xml so should init log later
    private static Logger log;

    public static RuntimeMode runtimeMode;
    public static DirProvider dirProvider;

    private static AplContainer container;

    private static AplCore core;

    private PropertiesHolder propertiesHolder;

    private void initCore() {

        AplCoreRuntime.getInstance().setup(runtimeMode, dirProvider);
        core = new AplCore();

        AplCoreRuntime.getInstance().addCore(core);
        core.init();
    }

    private void initUpdater(String attachmentFilePath, boolean debug) {
        if (!propertiesHolder.getBooleanProperty("apl.allowUpdates", false)) {
            return;
        }
        UpdaterCore updaterCore = CDI.current().select(UpdaterCoreImpl.class).get();

        updaterCore.init(attachmentFilePath, debug);
    }

    private void initAppStatusMsg() {
        AppStatus.setUpdater(new AppStatusUpdater() {
            @Override
            public void updateStatus(String status) {
                runtimeMode.updateAppStatus(status);
            }

            @Override
            public void alert(String message) {
                runtimeMode.alert(message);
            }

            @Override
            public void error(String message) {
                runtimeMode.displayError(message);
            }
        });
    }

    private void launchDesktopApplication() {
        runtimeMode.launchDesktopApplication();
    }

    public static void shutdown() {
        AplCoreRuntime.getInstance().shutdown();
        try {
            container.shutdown();
        } catch (IllegalStateException e) {
            log.error("Weld is stopped");
        }
    }

    public static PredefinedDirLocations merge(CmdLineArgs args, EnvironmentVariables vars, CustomDirLocations customDirLocations) {
        return new PredefinedDirLocations(
                customDirLocations.getDbDir().isEmpty()    ? StringUtils.isBlank(args.dbDir) ? vars.dbDir  : args.dbDir : customDirLocations.getDbDir().get(),
                StringUtils.isBlank(args.logDir)           ? vars.logDir           : args.logDir,
                customDirLocations.getKeystoreDir().isEmpty() ? StringUtils.isBlank(args.vaultKeystoreDir) ? vars.vaultKeystoreDir : args.vaultKeystoreDir : customDirLocations.getKeystoreDir().get(),
                StringUtils.isBlank(args.pidFile)          ? vars.pidFile          : args.pidFile,
                StringUtils.isBlank(args.twoFactorAuthDir) ? vars.twoFactorAuthDir : args.twoFactorAuthDir
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
        }
        catch (RuntimeException ex) {
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

//load configuration files
        EnvironmentVariables envVars = new EnvironmentVariables(Constants.APPLICATION_DIR_NAME);
        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(args.serviceMode, Constants.APPLICATION_DIR_NAME);

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
        CustomDirLocations customDirLocations = new CustomDirLocations(getCustomDbPath(chainId, props), props.getProperty(CustomDirLocations.KEYSTORE_DIR_PROPERTY_NAME));
        dirProvider = DirProviderFactory.getProvider(args.serviceMode, chainId, Constants.APPLICATION_DIR_NAME, merge(args,envVars, customDirLocations));
        RuntimeEnvironment.getInstance().setDirProvider(dirProvider);
        //init logging
        logDirPath = dirProvider.getLogsDir().toAbsolutePath();

        log = LoggerFactory.getLogger(Apollo.class);
        
//check webUI
        System.out.println("=== Bin directory is: " + DirProvider.getBinDir().toAbsolutePath());
/* at the moment we do it in build time

        Future<Boolean> unzipRes;
        WebUiExtractor we = new WebUiExtractor(dirProvider);
        ExecutorService execService = Executors.newFixedThreadPool(1);
        unzipRes = execService.submit(we);
*/

        runtimeMode = RuntimeEnvironment.getInstance().getRuntimeMode();
        runtimeMode.init();
        //init CDI container
        container = AplContainer.builder().containerId("MAIN-APL-CDI")
                .recursiveScanPackages(AplCore.class)
                .recursiveScanPackages(PropertiesHolder.class)
                .recursiveScanPackages(Updater.class)
                .recursiveScanPackages(ServerInfoEndpoint.class)
                .recursiveScanPackages(ServerInfoService.class)
                .recursiveScanPackages(Account.class)
                .recursiveScanPackages(TransactionType.class)
                .recursiveScanPackages(FullTextTrigger.class)
                .recursiveScanPackages(BlockchainConfig.class)
                .recursiveScanPackages(DatabaseManager.class)
                .recursiveScanPackages(DerivedTablesRegistry.class)
                .recursiveScanPackages(FullTextConfig.class)
                .annotatedDiscoveryMode()
                .interceptors(JdbiTransactionalInterceptor.class)
                .recursiveScanPackages(JdbiHandleFactory.class)
                .annotatedDiscoveryMode()
//TODO:  turn it on periodically in development processto check CDI errors
//                .devMode() // enable for dev only
                .build();

        // init config holders
        app.propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
        app.propertiesHolder.init(props);
        ChainsConfigHolder chainsConfigHolder = CDI.current().select(ChainsConfigHolder.class).get();
        chainsConfigHolder.setChains(chains);
        BlockchainConfigUpdater blockchainConfigUpdater = CDI.current().select(BlockchainConfigUpdater.class).get();
        blockchainConfigUpdater.updateChain(chainsConfigHolder.getActiveChain());
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(Apollo::shutdown, "ShutdownHookThread"));
            app.initAppStatusMsg();
            app.initCore();
            app.launchDesktopApplication();
            app.initUpdater(args.updateAttachmentFile, args.debug);
/*            if(unzipRes.get()!=true){
                System.err.println("Error! WebUI is not installed!");
            }
*/  
            if(args.startMint){
                AplCoreRuntime.getInstance().startMinter(); 
            }
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
            t.printStackTrace();
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

}
