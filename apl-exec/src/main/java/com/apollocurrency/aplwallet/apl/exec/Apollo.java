package com.apollocurrency.aplwallet.apl.exec;

import com.apollocurrency.aplwallet.apl.core.app.AplCore;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.AplGlobalObjects;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.app.UpdaterMediatorImpl;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.core.UpdaterCoreImpl;
import com.apollocurrency.aplwallet.apl.util.AppStatus;
import com.apollocurrency.aplwallet.apl.util.AppStatusUpdater;
import com.apollocurrency.aplwallet.apl.util.cdi.AplContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.apollocurrency.aplwallet.apl.util.env.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.env.PropertiesLoader;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apldesktop.DesktopMode;
import com.beust.jcommander.JCommander;
import java.util.Arrays;

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
    private static AplGlobalObjects aplGlobalObjects; // TODO: YL remove static later

    private static PropertiesLoader propertiesLoader;
    private PropertiesHolder propertiesHolder;
    
    private void initCore() {
                propertiesLoader.loadSystemProperties(
                        Arrays.asList(
                                "socksProxyHost",
                                "socksProxyPort",
                                "apl.enablePeerUPnP"));
        
        AplCoreRuntime.getInstance().setup(runtimeMode, dirProvider);
        core = new AplCore();
        AplCoreRuntime.getInstance().addCore(core);
        core.init();
    }

    private void initUpdater() {
        if (aplGlobalObjects == null) {
            aplGlobalObjects = CDI.current().select(AplGlobalObjects.class).get();
        }
        if (!propertiesHolder.getBooleanProperty("apl.allowUpdates", false)) {
            return;
        }
        UpdaterMediator mediator = new UpdaterMediatorImpl();
        UpdaterCore updaterCore = new UpdaterCoreImpl(mediator);
        AplGlobalObjects.createUpdaterCore(true, updaterCore);
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

        dirProvider = RuntimeEnvironment.getDirProvider();
        
//load configuration files        
        propertiesLoader = new PropertiesLoader(dirProvider);
        propertiesLoader.init();
//init logging        
        logDir = dirProvider.getLogFileDir().getAbsolutePath();
        log = LoggerFactory.getLogger(Apollo.class);
        
//TODO: remove this plumb, descktop UI should be separated and should not use Core directly but via API            
        if (RuntimeEnvironment.isDesktopApplicationEnabled()) {
            runtimeMode = new DesktopMode();
        } else {
            runtimeMode = RuntimeEnvironment.getRuntimeMode();
        }
        runtimeMode.init();
        //inti CDI container
        container = AplContainer.builder().containerId("MAIN-APL-CDI")
                .recursiveScanPackages(AplCore.class)
                .recursiveScanPackages(PropertiesHolder.class)
                .annotatedDiscoveryMode().build();        
        app.propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
        app.propertiesHolder.init(propertiesLoader.getProperties());

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(Apollo::shutdown));

            app.initAppStatusMsg();
            app.initCore();
            app.launchDesktopApplication();
            app.initUpdater();

        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
            t.printStackTrace();
        }
    }

}
