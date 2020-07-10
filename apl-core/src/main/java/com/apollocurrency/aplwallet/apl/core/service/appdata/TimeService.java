/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata;

public interface TimeService {
    int getEpochTime();

    /**
     * @return seconds since unix epoch
     */
    long systemTime();

    /**
     * @return milliseconds since unix epoch
     */
    long systemTimeMillis();

}
