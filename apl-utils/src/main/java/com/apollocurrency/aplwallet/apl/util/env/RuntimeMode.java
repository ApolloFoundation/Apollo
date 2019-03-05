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

public interface RuntimeMode {

    void init();

    void setServerStatus(ServerStatus status, URI wallet, File logFileDir);


    void shutdown();

    void alert(String message);
//
//    default void recoverDb() {
//        alert("Db Failed! Try to manually remove it.");
//    }

    default void updateAppStatus(String newStatus) {}

    void displayError(String errorMessage);
    
    default void launchDesktopApplication(){
    }   
}
