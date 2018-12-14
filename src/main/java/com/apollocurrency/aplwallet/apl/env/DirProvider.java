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

package com.apollocurrency.aplwallet.apl.env;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public interface DirProvider {

    boolean isLoadPropertyFileFromUserDir();

    default String getDbDir(String dbRelativeDir, UUID chainId, boolean chainIdFirst) {
        String chainIdDir = chainId == null ? "" : String.valueOf(chainId);
        Path dbDirRelativePath = Paths.get(dbRelativeDir);
        Path userHomeDirPath = Paths.get(getUserHomeDir());
        Path dbPath;
        if (chainIdFirst) {
            dbPath = userHomeDirPath
                    .resolve(chainIdDir)
                    .resolve(dbDirRelativePath);
        } else {
            dbPath = userHomeDirPath
                    .resolve(dbDirRelativePath)
                    .resolve(chainIdDir);
        }
        return dbPath.toString();
    }

    File getLogFileDir();

    File getConfDir();

    default File getKeystoreDir(String keystoreDir) {
        return new File(getUserHomeDir(), keystoreDir);
    }



    String getUserHomeDir();

    default File getBinDirectory() {
        return Paths.get("").toFile();
    }
}
