/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model.smc;

import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.contract.fuel.OutOfFuelException;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class AplFuel implements Fuel {
    private BigInteger fuelLimit;

    private BigInteger fuelPrice;

    private BigInteger fuelRemaining;

    @Override
    public BigInteger limit() {
        return fuelLimit;
    }

    @Override
    public BigInteger remaining() {
        return fuelRemaining;
    }

    @Override
    public BigInteger price() {
        return fuelPrice;
    }

    @Override
    public boolean tryToCharge(BigInteger bigInteger) {
        return false;
    }

    @Override
    public void charge(BigInteger bigInteger) throws OutOfFuelException {

    }

    @Override
    public void chargeFee(BigInteger bigInteger) throws OutOfFuelException {

    }

    @Override
    public void refund(BigInteger bigInteger) throws OutOfFuelException {

    }
}
