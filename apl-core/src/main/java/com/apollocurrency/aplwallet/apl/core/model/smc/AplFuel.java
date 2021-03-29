/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model.smc;

import com.apollocurrency.smc.contract.fuel.ContractFuel;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class AplFuel extends ContractFuel {
    public AplFuel(BigInteger limit, BigInteger price, BigInteger remaining) {
        super(limit, price, remaining);
    }


}
