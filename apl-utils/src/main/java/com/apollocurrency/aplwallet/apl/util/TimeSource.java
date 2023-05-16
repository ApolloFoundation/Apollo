/*
 *  Copyright © 2018-2022 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

/**
 * Define a time source which supply a Unix epoch time in milliseconds
 * @author Andrii Boiarskyi
 * @since 1.51.1
 */
public interface TimeSource {

    /**
     * @return UNIX epoch time in milliseconds (UTC, no timezone)
     */
    long currentTime();
}