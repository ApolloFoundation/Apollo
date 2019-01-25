package com.apollocurrency.aplwallet.apl.exec;

import com.apollocurrency.aplwallet.apl.core.app.AplCore;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainIdServiceImpl;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.updater.core.Updater;
import com.apollocurrency.aplwallet.apl.updater.core.UpdaterCoreImpl;
import com.apollocurrency.aplwallet.apl.util.AppStatus;
import com.apollocurrency.aplwallet.apl.util.AppStatusUpdater;
import com.apollocurrency.aplwallet.apl.util.cdi.AplContainer;
import com.apollocurrency.aplwallet.apl.util.env.PropertiesLoader;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.PredefinedDirLocations;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apldesktop.DesktopMode;
import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import javax.enterprise.inject.spi.CDI;

/**
 * Main Apollo startup class
 *
 * @author alukin@gmail.com
 */
public class Apollo {
    //This variable is used in LogDirPropertyDefiner configured in logback.xml
    public static String logDir=".";
    //We have dir provider configured in logback.xml so should init log later
    private static Logger log;

    public static RuntimeMode runtimeMode;
    public static DirProvider dirProvider;
    
    private static AplContainer container;
    
    private static AplCore core;

    private static PropertiesLoader propertiesLoader;
    private PropertiesHolder propertiesHolder;

    public static PropertiesLoader getPropertiesLoader() {
        return propertiesLoader;
    }

    private void initCore() {
                propertiesLoader.loadSystemProperties(
                        Arrays.asList(
                                "socksProxyHost",
                                "socksProxyPort",
                                "apl.enablePeerUPnP"));
        
        AplCoreRuntime.getInstance().setup(runtimeMode, dirProvider);
        core = new AplCore(CDI.current().select(BlockchainConfig.class).get());
        AplCoreRuntime.getInstance().addCore(core);
        core.init();
    }

    private void initUpdater() {
        if (!propertiesHolder.getBooleanProperty("apl.allowUpdates", false)) {
            return;
        }
        UpdaterCore updaterCore = CDI.current().select(UpdaterCoreImpl.class).get();
        updaterCore.init();
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
        container.shutdown(); 
        AplCoreRuntime.getInstance().shutdown();
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
        System.setProperty("apl.runtime.mode", args.serviceMode ? "service" : "user");
        RuntimeEnvironment.getInstance().setMain(Apollo.class);


        if(RuntimeEnvironment.getInstance().isAdmin()){
            System.out.println("==== RUNNING WITH ADMIN/ROOT PRIVILEGES! ====");
        }
//load configuration files        
        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(args.serviceMode, Constants.APPLICATION_DIR_NAME);
        propertiesLoader = new PropertiesLoader(configDirProvider,
                args.isResourceIgnored(),
                args.configDir);
// init application data dir provider
        EnvironmentVariables environmentVariables = new EnvironmentVariables(Constants.APPLICATION_DIR_NAME);
        dirProvider = createDirProvider(environmentVariables.merge(args), args.serviceMode);
        //init logging
        logDir = dirProvider.getLogsDir().toAbsolutePath().toString();
        log = LoggerFactory.getLogger(Apollo.class);
//check webUI
        System.out.println("=== Bin directory is: "+dirProvider.getBinDir().toAbsolutePath());
/* at the moment we do it in build time
        Future<Boolean> unzipRes;
        WebUiExtractor we = new WebUiExtractor(dirProvider);
        ExecutorService execService = Executors.newFixedThreadPool(1);
        unzipRes = execService.submit(we);
*/
//TODO: remove this plumb, desktop UI should be separated and should not use Core directly but via API
        if (RuntimeEnvironment.getInstance().isDesktopApplicationEnabled()) {
            runtimeMode = new DesktopMode();
        } else {
            runtimeMode = RuntimeEnvironment.getInstance().getRuntimeMode();
        }
        runtimeMode.init();
        //inti CDI container
        container = AplContainer.builder().containerId("MAIN-APL-CDI")
                .recursiveScanPackages(AplCore.class)
                .recursiveScanPackages(PropertiesHolder.class)
                .recursiveScanPackages(Updater.class)
                .annotatedDiscoveryMode().build();
        app.propertiesHolder = CDI.current().select(PropertiesHolder.class).get();

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(Apollo::shutdown, "ShutdownHookThread"));
            app.initAppStatusMsg();
            app.initCore();
            app.launchDesktopApplication();
            app.initUpdater();
/*            if(unzipRes.get()!=true){
                System.err.println("Error! WebUI is not installed!");
            }
*/
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
            t.printStackTrace();
        }
    }

    private static DirProvider createDirProvider(PredefinedDirLocations dirLocations, boolean isService) {
        try {
            ChainIdServiceImpl chainIdService = new ChainIdServiceImpl();
            UUID chainId = chainIdService.getActiveChain().getChainId();
            return new DirProviderFactory().getInstance(isService, chainId, Constants.APPLICATION_DIR_NAME, dirLocations);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to create dirProvider, cannot load chains config", e);
        }
    }
}
