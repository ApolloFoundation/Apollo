/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Properties;

import static org.slf4j.LoggerFactory.getLogger;

public class UpdaterConstants {
    private static final Logger LOG = getLogger(UpdaterConstants.class);
    private static final Properties updaterProperties;

    static {
        updaterProperties = new Properties();
        //TODO: check it, there's no "conf" directory anymore
        try (InputStream is = UpdaterConstants.class.getClassLoader().getResourceAsStream("conf/updater.properties")) {
            updaterProperties.load(is);
        }
        catch (Throwable e) {
            LOG.error("Unable to load Updater config", e);
        }
    }
    // Platform dependent updater constants
    //TODO: check update scripts executions
   public static final String WINDOWS_UPDATE_SCRIPT_PATH = "updater\\update.vbs";
   public static final String LINUX_UPDATE_SCRIPT_PATH = "updater/update.sh";
   public static final String MAC_OS_UPDATE_SCRIPT_PATH = "updater/update.sh";
   // run tools for update scripts
   public static final String WINDOWS_RUN_TOOL_PATH = getPropertyOrDefault("updater.platformDependentUpdater.windowsRunToolPath", "cscript.exe");
   public static final String LINUX_RUN_TOOL_PATH = getPropertyOrDefault("updater.platformDependentUpdater.linuxRunToolPath", "/bin/bash");
   public static final String MAC_OS_RUN_TOOL_PATH = getPropertyOrDefault("updater.platformDependentUpdater.macOSRunToolPath", "/bin/bash");

   // Downloader constants
   public static final int DOWNLOAD_ATTEMPTS = getIntPropertyOrDefault("updater.downloader.numberOfAttempts", 10);
   public static final int NEXT_ATTEMPT_TIMEOUT = getIntPropertyOrDefault("updater.downloader.attemptTimeout", 60);
   public static final String TEMP_DIR_PREFIX = "Apollo-update";
   public static final String DOWNLOADED_FILE_NAME = "Apollo.jar";
   public static final int MAX_SHUTDOWN_TIMEOUT = 5; //seconds

    // Certificate constants (AuthorityCheckerImpl)
   public static final String CERTIFICATE_DIRECTORY = "conf/certs";
   public static final String FIRST_DECRYPTION_CERTIFICATE_PREFIX = "1_";
   public static final String SECOND_DECRYPTION_CERTIFICATE_PREFIX = "2_";
   public static final String CERTIFICATE_SUFFIX = ".crt";
   public static final String INTERMEDIATE_CERTIFICATE_NAME = "intermediate" + CERTIFICATE_SUFFIX;
   public static final String CA_CERTIFICATE_NAME = "rootCA" + CERTIFICATE_SUFFIX;
   public static final String CA_CERTIFICATE_URL = "https://raw.githubusercontent.com/ApolloFoundation/Apollo/master/conf/certs/" + CA_CERTIFICATE_NAME;

   //'Important update' constants
    public static final int MIN_BLOCKS_DELAY = getIntPropertyOrDefault("updater.importantUpdate.minBlocksWaiting", 10);
    public static final int MAX_BLOCKS_DELAY = getIntPropertyOrDefault("updater.importantUpdate.maxBlocksWaiting", 20);

   private UpdaterConstants() {}
    private static String getPropertyOrDefault(String propertyName, String defaultValue) {
        if (updaterProperties != null) {
            String property = updaterProperties.getProperty(propertyName);
            if (property != null) {
                return property;

            }
        }
        return defaultValue;
    }

    private static int getIntPropertyOrDefault(String propertyName, int defaultValue) {
        return Integer.parseInt(getPropertyOrDefault(propertyName, String.valueOf(defaultValue)));
    }
}
