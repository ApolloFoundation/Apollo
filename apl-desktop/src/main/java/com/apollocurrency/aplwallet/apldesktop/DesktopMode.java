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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javafx.application.Platform;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static org.slf4j.LoggerFactory.getLogger;

public class DesktopMode {
    private static Logger LOG;

    private static DesktopSystemTray desktopSystemTray;
    private static DesktopApplication desktopApp;
    private static String OS = System.getProperty("os.name").toLowerCase();

    
    public static void main(String[] args) {
        LOG = getLogger(DesktopMode.class);        
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
            desktopApp.startDesktopApplication();
           
        };
        
        Thread updateSplashScreenThread = new Thread(statusUpdater, "SplashScreenStatusUpdaterThread");
        updateSplashScreenThread.setDaemon(true);
        updateSplashScreenThread.start();
        LookAndFeel.init();
        desktopApp = new DesktopApplication();            
        Thread desktopAppThread = new Thread(() -> {
                desktopApp.launch();
        });
        desktopAppThread.start();
        desktopSystemTray = new DesktopSystemTray();
        SwingUtilities.invokeLater(desktopSystemTray::createAndShowGUI);
    }

    private static boolean checkAPI()
    {
        OkHttpClient client = new OkHttpClient();
        String url = "http://localhost:7876/";
        Request request = new Request.Builder().url(url).build();

        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException ex) {
            //java.util.logging.Logger.getLogger(DesktopMode.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return response.code() == 200;    
    }
    
    public void setServerStatus(ServerStatus status, URI wallet, File logFileDir) {
        desktopSystemTray.setToolTip(new SystemTrayDataProvider(status.getMessage(), wallet, logFileDir));
    }

    public void launchDesktopApplication() {
        LOG.info("Launching desktop wallet");
            desktopApp.startDesktopApplication();
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
            
            if (OS.indexOf("win") >= 0 ) 
            {
                
                p = Runtime.getRuntime().exec("./bin/apl-run.bat");
                
            }
            else{
                p = Runtime.getRuntime().exec("/usr/bin/bash ./bin/apl-run.sh");
                //p.waitFor();
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
