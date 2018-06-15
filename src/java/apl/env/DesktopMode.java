/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package apl.env;

import apl.util.Logger;
import apldesktop.DesktopApplication;

import javax.swing.*;
import java.io.File;
import java.net.URI;

public class DesktopMode implements RuntimeMode {

    private DesktopSystemTray desktopSystemTray;
    private Thread desktopAppThread;
    @Override
    public void init() {
        LookAndFeel.init();
//        desktopApplication = DesktopApplication.getInstance();
        desktopAppThread = new Thread(DesktopApplication::launch);
        desktopAppThread.start();
        desktopSystemTray = new DesktopSystemTray();
        SwingUtilities.invokeLater(desktopSystemTray::createAndShowGUI);
    }

    @Override
    public void setServerStatus(ServerStatus status, URI wallet, File logFileDir) {
        desktopSystemTray.setToolTip(new SystemTrayDataProvider(status.getMessage(), wallet, logFileDir));
    }

    @Override
    public void launchDesktopApplication() {
        Logger.logInfoMessage("Launching desktop wallet");
        DesktopApplication.startDesktopApplication();
    }

    @Override
    public void shutdown() {
        desktopSystemTray.shutdown();
        DesktopApplication.shutdown();
    }

    @Override
    public void alert(String message) {
        desktopSystemTray.alert(message);
    }

    @Override
    public void recoverDb() {
        DesktopApplication.recoverDbUI();
    }

    @Override
    public void updateAppStatus(String newStatus) {
        DesktopApplication.updateSplashScreenStatus(newStatus);
    }
}
