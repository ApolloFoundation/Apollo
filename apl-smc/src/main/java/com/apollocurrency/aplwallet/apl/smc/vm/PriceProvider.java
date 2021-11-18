/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.vm;

import com.apollocurrency.smc.contract.fuel.OperationPrice;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface PriceProvider {
    OperationPrice getPrice(int height);
}
