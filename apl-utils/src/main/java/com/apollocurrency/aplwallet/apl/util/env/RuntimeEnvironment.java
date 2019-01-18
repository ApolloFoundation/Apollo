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
package com.apollocurrency.aplwallet.apl.util.env;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.prefs.Preferences;

public class RuntimeEnvironment {

    public static final String RUNTIME_MODE_ARG = "apl.runtime.mode";

    private static final String osname = System.getProperty("os.name").toLowerCase();
    private static final boolean isHeadless;
    protected static final boolean hasJavaFX;
    private static boolean isServiceMode = false;

    static {
        boolean b;
        try {
            // Load by reflection to prevent exception in case java.awt does not exist
            Class graphicsEnvironmentClass = Class.forName("java.awt.GraphicsEnvironment");
            Method isHeadlessMethod = graphicsEnvironmentClass.getMethod("isHeadless");
            b = (Boolean) isHeadlessMethod.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            b = true;
        }
        isHeadless = b;
        try {
            Class.forName("javafx.application.Application");
            b = true;
        } catch (ClassNotFoundException e) {
            System.out.println("javafx not supported");
            b = false;
        }
        hasJavaFX = b;
        isServiceMode = isServiceMode();
    }

    public static boolean isWindowsRuntime() {
        return osname.startsWith("windows");
    }

    public static boolean isUnixRuntime() {
        return osname.contains("nux") || osname.contains("nix") || osname.contains("aix") || osname.contains("bsd") || osname.contains("sunos");
    }

    public static boolean isMacRuntime() {
        return osname.contains("mac");
    }

    public static boolean isServiceMode() {
        return "service".equalsIgnoreCase(System.getProperty(RUNTIME_MODE_ARG));
    }

    public static boolean isHeadless() {
        return isHeadless;
    }
    
/**
 * Not very good but working method to get info about user super privileges
 * @return true if current user has admin/root provilieges
 */
    public static boolean isAdmin() {
        Preferences prefs = Preferences.systemRoot();
        PrintStream systemErr = System.err;
        synchronized (systemErr) {    // better synchroize to avoid problems with other threads that access System.err
            System.setErr(null);
            try {
                prefs.put("foo", "bar"); // SecurityException on Windows
                prefs.remove("foo");
                prefs.flush(); // BackingStoreException on Linux
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                System.setErr(systemErr);
            }
        }
    }

    public static RuntimeMode getRuntimeMode() {
        System.out.println("isHeadless=" + isHeadless());
        if (isServiceMode()) {
            return new ServiceMode();
        } else {
            return new UserMode();
        }
    }

    public static boolean isDesktopEnabled() {
        return "desktop".equalsIgnoreCase(System.getProperty(RUNTIME_MODE_ARG)) && !isHeadless();
    }

    public static boolean isDesktopApplicationEnabled() {
        return isDesktopEnabled() && hasJavaFX;
    }

}
