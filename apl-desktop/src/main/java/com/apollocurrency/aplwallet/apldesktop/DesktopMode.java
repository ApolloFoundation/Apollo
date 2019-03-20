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

package com.apollocurrency.aplwallet.apldesktop;

import com.apollocurrency.aplwallet.apl.util.env.ServerStatus;
import org.slf4j.Logger;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import static org.slf4j.LoggerFactory.getLogger;

public class DesktopMode {
    public static String logDir = ".";
    private static Logger LOG;

    private static DesktopSystemTray desktopSystemTray;
    private static DesktopApplication desktopApp;
    private static String OS = System.getProperty("os.name").toLowerCase();
    private static String APIUrl;
    
    public static void main(String[] args) {
        
        //TODO: Adopt to config files
        /*/load configuration files
        EnvironmentVariables envVars = new EnvironmentVariables(Constants.APPLICATION_DIR_NAME);
        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(false, Constants.APPLICATION_DIR_NAME);

        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
                configDirProvider,
                false,
                envVars.configDir,
                Constants.DESKTOP_APPLICATION_NAME + ".properties",
                null
        );
        
        // init config holders
        container.builder().containerId("APL-DESKTOP-CDI")
                .recursiveScanPackages(PropertiesHolder.class)
                .annotatedDiscoveryMode().build();        

        properties = CDI.current().select(PropertiesHolder.class).get();
        LOG.debug("PROPERTIES2:" + properties.getClass().getName());
        Properties props = propertiesLoader.load();
        properties.init(props);
        
        // init application data dir provider
        LOG.debug("PROPERTIES:" + props.toString());
        
        dirProvider = DirProviderFactory.getProvider(
                false, 
                UUID.fromString("d5c22b16-935e-495d-aa3f-bb26ef2115d3"), // stub for compatibility
                Constants.APPLICATION_DIR_NAME, 
                new PredefinedDirLocations(null, logDir, null, null, null));
        RuntimeEnvironment.getInstance().setDirProvider(dirProvider);
        //init logging
        logDir = dirProvider.getLogsDir().toAbsolutePath().toString();
        */
        LOG = getLogger(DesktopMode.class);        
        
        desktopApp = new DesktopApplication();            
        Thread desktopAppThread = new Thread(() -> {
                desktopApp.launch();
        });
        desktopAppThread.start();
        new Thread(() -> runBackend()).start();
        Runnable statusUpdater = () -> {
            while (!checkAPI()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                }
                catch (InterruptedException e) {
                    LOG.info("GUI thread was interrupted", e);
                }
            }
            desktopApp.startDesktopApplication(APIUrl);
           
        };
        
        Thread updateSplashScreenThread = new Thread(statusUpdater, "SplashScreenStatusUpdaterThread");
        updateSplashScreenThread.setDaemon(true);
        updateSplashScreenThread.start();
        LookAndFeel.init();
        
        desktopSystemTray = new DesktopSystemTray();
        SwingUtilities.invokeLater(desktopSystemTray::createAndShowGUI);
    }

    private static boolean checkAPI()
    {
        OkHttpClient client = new OkHttpClient();
        //String url = properties.getStringProperty("apl.APIURL");
        //TODO: This code was written when I was very tired, resolvin CDI NPE...
        
        String[] APIPorts = {"6876", "7876"};
        //String url = properties.getStringProperty("apl.APIURL");
        String url = "http://localhost:" + APIPorts[new Random().nextInt(1)] + "/";
        Request request = new Request.Builder().url(url).build();

        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException ex) {
            //java.util.logging.Logger.getLogger(DesktopMode.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        if( response.code() == 200){
            APIUrl = url;
            return true;
        }
        return false;
    }
    
    public void setServerStatus(ServerStatus status, URI wallet, File logFileDir) {
        desktopSystemTray.setToolTip(new SystemTrayDataProvider(status.getMessage(), wallet, logFileDir));
    }

    public void launchDesktopApplication() {
        LOG.info("Launching desktop wallet");
            desktopApp.startDesktopApplication(APIUrl);
    }

    
    public void shutdown() {
        desktopSystemTray.shutdown();
        desktopApp.shutdown();
    }

    
    public void alert(String message) {
        desktopSystemTray.alert(message);
    }

//    @Override
//    public void recoverDb() {
//        try {
//            desktopAppClass.getMethod("recoverDbUI").invoke(null);
//        }
//        catch (Exception e) {
//            //rethrow
//            throw new RuntimeException("Unable to show recover db dialog!", e);
//        }
//    }

    
    public void updateAppStatus(String newStatus) {
        desktopApp.updateSplashScreenStatus(newStatus);
    }

    private static void runBackend(){
        Process p;
        try{
            
            if (System.getProperty("os.name").toLowerCase().contains("win")) 
            {

                ProcessBuilder pb = new ProcessBuilder(".\\apl-run.bat")
                // Some magic: Without Redirect Output will not work on windows
                        .redirectOutput(new File(System.getProperty("java.io.tmpdir") + "\\Apollo-Output.log"))
                        .redirectError(new File(System.getProperty("java.io.tmpdir") + "\\Apollo-Error.log"));
                pb.start();
            }
            else{
                ProcessBuilder pb = new ProcessBuilder("/bin/bash", "./apl-start.sh");
                pb.start();

            }
            
        }            
        catch (IOException e)
        {
            LOG.debug(e.getMessage());
        }/* catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(DesktopMode.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }
    
    public void displayError(String errorMessage) {
            desktopApp.showError(errorMessage);
    }
}
