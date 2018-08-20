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

public enum ServerStatus {
    BEFORE_DATABASE("Loading Database"), AFTER_DATABASE("Loading Resources"), STARTED("Online"), SHUTDOWN("Offline");

    private final String message;

    ServerStatus(String message) {
        this.message = message;
    }

    ServerStatus() {
        message= "";
    }

    public String getMessage() {
        return message;
    }
}
