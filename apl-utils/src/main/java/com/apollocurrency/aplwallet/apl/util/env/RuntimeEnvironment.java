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
 * Copyright © 2018 - 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.prefs.Preferences;

import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;

public class RuntimeEnvironment {

    public static final String RUNTIME_MODE_ARG = "apl.runtime.mode";
    public static final String DIRPROVIDER_ARG = "apl.runtime.dirProvider";

    private static final String osname = System.getProperty("os.name").toLowerCase();
    private  boolean isHeadless;
    protected boolean hasJavaFX;
    private static boolean isServiceMode = false;
    private static RuntimeEnvironment instance = null;
    private Class mainClass=null;
    private DirProvider dirProvider;
    
    void setup() {
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

    public static RuntimeEnvironment getInstance(){
        if(instance==null){
            instance = new RuntimeEnvironment();
        }
        return instance;
    }

    private RuntimeEnvironment(){
         setup();
    }

    public boolean isWindowsRuntime() {
        return osname.startsWith("windows");
    }

    public boolean isUnixRuntime() {
        return osname.contains("nux") || osname.contains("nix") || osname.contains("aix") || osname.contains("bsd") || osname.contains("sunos");
    }

    public  boolean isMacRuntime() {
        return osname.contains("mac");
    }

    public  boolean isServiceMode() {
        return "service".equalsIgnoreCase(System.getProperty(RUNTIME_MODE_ARG));
    }

    public boolean isHeadless() {
        return isHeadless;
    }
    
/**
 * Not very good but working method to get info about user super privileges
 * @return true if current user has admin/root provilieges
 */
    public boolean isAdmin() {
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

    public RuntimeMode getRuntimeMode() {
        if (isServiceMode()) {
            return new ServiceMode();
        } else {
            return new UserMode();
        }
    }

    public boolean isDesktopEnabled() {
        return "desktop".equalsIgnoreCase(System.getProperty(RUNTIME_MODE_ARG)) && !isHeadless();
    }

    public boolean isDesktopApplicationEnabled() {
        return isDesktopEnabled() && hasJavaFX;
    }

    public DirProvider getDirProvider() {
        return dirProvider;
    }

    public void setDirProvider(DirProvider dirProvider) {
        this.dirProvider = dirProvider;
    }

    public void setMain(Class aClass) {
        mainClass=aClass;
    }
    public Class getMain(){
        return mainClass;
    }
}
