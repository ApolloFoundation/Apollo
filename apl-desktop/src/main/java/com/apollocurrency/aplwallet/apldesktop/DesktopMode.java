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

import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apl.util.env.ServerStatus;
import org.slf4j.Logger;

import javax.swing.*;
import java.io.File;
import java.net.URI;

import static org.slf4j.LoggerFactory.getLogger;

public class DesktopMode implements RuntimeMode {
    private static Logger LOG;

    private DesktopSystemTray desktopSystemTray;
    private DesktopApplication desktopApp;

    @Override
    public void init() {
        LOG = getLogger(DesktopMode.class);        
            LookAndFeel.init();
            desktopApp = new DesktopApplication();            
            Thread desktopAppThread = new Thread(() -> {
                    desktopApp.launch();
            });
            desktopAppThread.start();
            desktopSystemTray = new DesktopSystemTray();
            SwingUtilities.invokeLater(desktopSystemTray::createAndShowGUI);
    }

    @Override
    public void setServerStatus(ServerStatus status, URI wallet, File logFileDir) {
        desktopSystemTray.setToolTip(new SystemTrayDataProvider(status.getMessage(), wallet, logFileDir));
    }

    public void launchDesktopApplication() {
        LOG.info("Launching desktop wallet");
            desktopApp.startDesktopApplication();
    }

    @Override
    public void shutdown() {
        desktopSystemTray.shutdown();
        desktopApp.shutdown();
    }

    @Override
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

    @Override
    public void updateAppStatus(String newStatus) {
        desktopApp.updateSplashScreenStatus(newStatus);
    }

    @Override
    public void displayError(String errorMessage) {
            desktopApp.showError(errorMessage);
    }
}
