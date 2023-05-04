/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.vm;

import com.apollocurrency.smc.contract.fuel.ConstantFuelCalculator;
import com.apollocurrency.smc.contract.fuel.FuelCalculator;
import com.apollocurrency.smc.contract.fuel.MemCostFuelCalculator;
import com.apollocurrency.smc.contract.fuel.VolumeBasedFuelCalculator;
import com.apollocurrency.smc.data.type.MemUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author andrew.zinchenko@gmail.com
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class FuelCost {

    public static final ConstantFuelCalculator F_ZERO = ConstantFuelCalculator.ZERO_FUEL_CALCULATOR;

    //publish new smart contract
    public static final MemCostFuelCalculator F_PUBLISH = new MemCostFuelCalculator(15_000_000, 1000, MemUnit.BYTE);

    //transfer coins from an address to another address
    public static final ConstantFuelCalculator F_SEND_MONEY = new ConstantFuelCalculator(10_000);

    //call the contract method
    public static final MemCostFuelCalculator F_CALL = new MemCostFuelCalculator(1_000_000, 1000, MemUnit.BYTE);

    //call another contract method from the contract
    public static final MemCostFuelCalculator F_SEND_MESSAGE = new MemCostFuelCalculator(1_000_000, 1000, MemUnit.BYTE);

    //math functions using
    public static final ConstantFuelCalculator F_HASH_SUM = new ConstantFuelCalculator(10_000);

    //state storage using and to refund for removing
    public static final MemCostFuelCalculator F_STATE_WRITE = new MemCostFuelCalculator(0L, 1000L, MemUnit.BYTE);

    //store value to Mapping
    public static final ConstantFuelCalculator F_MAPPING_CREATE = new ConstantFuelCalculator(10_000L);
    public static final FuelCalculator F_MAPPING_READ = F_ZERO;//read operation must be free
    public static final MemCostFuelCalculator F_MAPPING_WRITE = new MemCostFuelCalculator(1000L, 100L, MemUnit.BYTE);
    //to refund for removing
    public static final MemCostFuelCalculator F_MAPPING_DELETE = new MemCostFuelCalculator(100L, 100L, MemUnit.BYTE);
    //to pay for UPDATE operation - sequentially charge a fee for DELETE and WRITE

    //Emit events
    //charge rate per event parameter
    public static final VolumeBasedFuelCalculator F_EVENT_EMIT = new VolumeBasedFuelCalculator(10_000L, 1000L);

    //Contract execution, on all statements entering
    public static final ConstantFuelCalculator F_STATEMENT_ENTER = new ConstantFuelCalculator(1L);

}
