/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.smc.data.type.Address;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcEventService {
    /**
     * Checks if the given contract exist
     *
     * @param contract the given contract address
     * @return true if the given contract already stored into the blockchain
     */
    boolean isExist(Address contract);
}
