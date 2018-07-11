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

package com.apollocurrency.aplwallet.apl.env;

import com.apollocurrency.aplwallet.apl.util.Logger;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;

public class DesktopMode implements RuntimeMode {

    private DesktopSystemTray desktopSystemTray;
    private Class desktopAppClass;

    @Override
    public void init() {
        try {
            LookAndFeel.init();
            desktopAppClass = Class.forName("com.apollocurrency.aplwallet.apldesktop.DesktopApplication");
            Method launchDesktopAppMethod = desktopAppClass.getMethod("launch");
            Thread desktopAppThread = new Thread(() -> {
                try {
                    launchDesktopAppMethod.invoke(null);
                }
                catch (IllegalAccessException | InvocationTargetException e) {
                    Logger.logErrorMessage("Unable to launch desktop application", e);
                }
            });
            desktopAppThread.start();
            desktopSystemTray = new DesktopSystemTray();
            SwingUtilities.invokeLater(desktopSystemTray::createAndShowGUI);
        }
        catch (ClassNotFoundException e) {
            Logger.logErrorMessage("Cannot find desktop application class", e);
        }
        catch (NoSuchMethodException e) {
            Logger.logErrorMessage("Missing 'launch' method to start desktop application", e);
        }
    }

    @Override
    public void setServerStatus(ServerStatus status, URI wallet, File logFileDir) {
        desktopSystemTray.setToolTip(new SystemTrayDataProvider(status.getMessage(), wallet, logFileDir));
    }

    @Override
    public void launchDesktopApplication() {
        Logger.logInfoMessage("Launching desktop wallet");
        try {
            desktopAppClass.getMethod("startDesktopApplication").invoke(null);
        }
        catch (Exception e) {
            //rethrow
            throw new RuntimeException("Cannot start desktop application", e);
        }
    }

    @Override
    public void shutdown() {
        desktopSystemTray.shutdown();
        try {
            desktopAppClass.getMethod("shutdown").invoke(null);
        }
        catch (Exception e) {
            //rethrow
            throw new RuntimeException("Cannot shutdown desktop application", e);
        }
    }

    @Override
    public void alert(String message) {
        desktopSystemTray.alert(message);
    }

    @Override
    public void recoverDb() {
        try {
            desktopAppClass.getMethod("recoverDbUI").invoke(null);
        }
        catch (Exception e) {
            //rethrow
            throw new RuntimeException("Unable to show recover db dialog!", e);
        }
    }

    @Override
    public void updateAppStatus(String newStatus) {
        try {
            desktopAppClass.getMethod("updateSplashScreenStatus", String.class).invoke(null, newStatus);
        }
        catch (Exception e) {
            //rethrow
            throw new RuntimeException("Unable to update status on splash screen!", e);
        }
    }
}
