/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.smc.ws.SmcEventService;
import com.apollocurrency.smc.data.type.Address;

import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class SmcEventServiceImpl implements SmcEventService {
    @Override
    public boolean isExist(Address contract) {
        return false;
    }
}
