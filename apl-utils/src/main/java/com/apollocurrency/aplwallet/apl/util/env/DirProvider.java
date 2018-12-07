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

import java.io.File;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

public interface DirProvider {

    public boolean isLoadPropertyFileFromUserDir();

    public void updateLogFileHandler(Properties loggingProperties);

    public String getDbDir(String dbRelativeDir, UUID chainId, boolean chainIdFirst);
    
    /**
     * Directory for log files for certain platform and run mode
     * @return File denoting path to directory where logs should go
     */
    public File getLogFileDir();
    
    /**
     * Directory for configuration files. Default configuration for all
     * platforms is in jar file resources. User or system administrator
     * provided configuration overrides default configuration values.
     * @return File denoting path to configuration files deirectory
     */
    public File getConfDir();
    
    /**
     * Directory where keys are stored
     * @param keystoreDir relative path from default key store directory
     * @return File denoting path to key store directory
     */
    public File getKeystoreDir(String keystoreDir);
    
    /**
     * User home directory on all platforms. It is user home and NEVER nothing else.
     * @return File denoting path to user home
     */
    public String getUserHomeDir();
   
   /**
    * Path to directory where main executable jar file is placed
    * @return File denoting path to directory with main executable jar
    */
    public default File getBinDirectory() {
        return Paths.get("").toFile();
    }
}
