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

import com.apollocurrency.aplwallet.apl.Db;
import com.apollocurrency.aplwallet.apl.util.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class CommandLineMode implements RuntimeMode {

    @Override
    public void init() {}

    @Override
    public void setServerStatus(ServerStatus status, URI wallet, File logFileDir) {}

    @Override
    public void launchDesktopApplication() {}

    @Override
    public void shutdown() {}

    @Override
    public void alert(String message) {}

    @Override
    public void recoverDb() {
        //simple db removing
        try {
            Db.tryToDeleteDb();
            Logger.logInfoMessage("Db was removed successfully. Please, restart the application!");
            System.exit(0);
        }
        catch (IOException e) {
            Logger.logErrorMessage("Cannot delete db", e);
            System.exit(1);
        }
    }
}
