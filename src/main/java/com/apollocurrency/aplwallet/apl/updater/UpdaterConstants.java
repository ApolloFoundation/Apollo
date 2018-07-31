/*
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

package com.apollocurrency.aplwallet.apl.updater;

public class UpdaterConstants {
   public static final String WINDOWS_UPDATE_SCRIPT_PATH = "update.vbs";
   public static final String LINUX_UPDATE_SCRIPT_PATH = "update.sh";
   public static final String OSX_UPDATE_SCRIPT_PATH = "update.sh";
   public static final String WINDOWS_RUN_TOOL_PATH = "wscript.exe";
   public static final String LINUX_RUN_TOOL_PATH = "/bin/bash";
   public static final String OSX_RUN_TOOL_PATH = "";
   public static final int DOWNLOAD_ATTEMPTS = 10;
   public static final int NEXT_ATTEMPT_TIMEOUT = 60;
   public static final String TEMP_DIR_PREFIX = "Apollo-update";
   public static final String DOWNLOADED_FILE_NAME = "Apollo-newVersion.jar";

//   Certificate constants
   public static final String CERTIFICATE_DIRECTORY = "conf/certs";
   public static final String FIRST_DECRYPTION_CERTIFICATE_PREFIX = "1_";
   public static final String SECOND_DECRYPTION_CERTIFICATE_PREFIX = "2_";
   public static final String CERTIFICATE_SUFFIX = ".crt";
   public static final String INTERMEDIATE_CERTIFICATE_NAME = "intermediate" + CERTIFICATE_SUFFIX;
   public static final String CA_CERTIFICATE_NAME = "rootCA" + CERTIFICATE_SUFFIX;
   public static final String CA_CERTIFICATE_URL = "https://raw.githubusercontent.com/ApolloFoundation/Apollo/master/conf/certs/" + CA_CERTIFICATE_NAME;

    public static final int MIN_BLOCKS_WAITING = 10;
    public static final int MAX_BLOCKS_WAITING = 20;

   private UpdaterConstants() {}
}
