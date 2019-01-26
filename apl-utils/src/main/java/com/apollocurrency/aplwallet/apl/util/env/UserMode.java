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
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UserMode implements RuntimeMode {
    private static Logger LOG = LoggerFactory.getLogger(UserMode.class);

    @Override
    public void init() {
    }

    @Override
    public void setServerStatus(ServerStatus status, URI wallet, File logFileDir) {
        LOG.info("Server status: {}", status.getMessage());
    }


    @Override
    public void shutdown() {}

    @Override
    public void alert(String message) {
        LOG.warn(message);
    }

//    @Override
//    public void recoverDb() {
        //simple db removing
//        try {
//TODO           Db.tryToDeleteDb();
//            LOG.info("Db was removed successfully. Please, restart the application!");
//            System.exit(0);
//        }
//        catch (IOException e) {
//            LOG.error("Cannot delete db", e);
//            System.exit(1);
//        }
//    }

    @Override
    public void updateAppStatus(String newStatus) {
        LOG.info("Application status: {}", newStatus);
    }

    @Override
    public void displayError(String errorMessage) {
        LOG.error(errorMessage);
    }
}
