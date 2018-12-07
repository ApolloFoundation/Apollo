package com.apollocurrency.aplwallet.apl.exec;

import com.apollocurrency.aplwallet.apl.core.app.AplCore;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.AplGlobalObjects;
import com.apollocurrency.aplwallet.apl.core.app.UpdaterMediatorImpl;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.core.UpdaterCoreImpl;
import com.apollocurrency.aplwallet.apl.util.AppStatus;
import com.apollocurrency.aplwallet.apl.util.AppStatusUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import com.apollocurrency.aplwallet.apl.util.env.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;

/**
 * Main Apollo startup class
 *
 * @author alukin@gmail.com
 */
public class Apollo {
    private static Logger log;// = LoggerFactory.getLogger(Apollo.class);

    public static  RuntimeMode runtimeMode;
    public static  DirProvider dirProvider;
    
    private static AplCore core;
    private static AplGlobalObjects aplGlobalObjects; // TODO: YL remove static later
    
    private void initCore() {
        AplCoreRuntime.getInstance().setup(runtimeMode, dirProvider);
        core = new AplCore();
        AplCoreRuntime.getInstance().addCore(core);
        core.init();
        
    }

    private static void printCommandLineArguments(String[] args) {
        System.out.println("Command line arguments: " + Arrays.toString(args));
    }

    private void initUpdater() {
        if (aplGlobalObjects == null) {
            aplGlobalObjects = CDI.current().select(AplGlobalObjects.class).get();
        }
        if (!aplGlobalObjects.getBooleanProperty("apl.allowUpdates", false)) {
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
        core.shutdown();
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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Initializing " + AplCore.APPLICATION + " server version " + AplCore.VERSION);
        printCommandLineArguments(args);
        Apollo app = new Apollo();
        runtimeMode = RuntimeEnvironment.getRuntimeMode();
        dirProvider = RuntimeEnvironment.getDirProvider();        
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(Apollo::shutdown));
            app.initCore();
            log = LoggerFactory.getLogger(Apollo.class);
//           redirectSystemStreams("out");
//            redirectSystemStreams("err");
            app.initAppStatusMsg();
            if (app.core.isDesktopApplicationEnabled()) {
                runtimeMode.updateAppStatus("Starting desktop application...");
                app.launchDesktopApplication();
            }
            app.initUpdater();

        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
            t.printStackTrace();
        }
    }

}
