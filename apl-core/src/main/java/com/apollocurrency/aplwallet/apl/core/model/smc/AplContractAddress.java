/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model.smc;

import com.apollocurrency.smc.SMCException;
import com.apollocurrency.smc.contract.vm.ContractAddress;
import com.apollocurrency.smc.data.type.Address;
import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Data
@Builder
public class AplContractAddress implements ContractAddress {
    private Address address; //account address

    @Override
    public BigInteger balance() {
        return null;
    }

    @Override
    public void transfer(BigInteger bigInteger) throws SMCException {

    }

    @Override
    public boolean send(BigInteger bigInteger) {
        return false;
    }

    @Override
    public byte[] get() {
        return address.get();
    }

    @Override
    public String getHex() {
        return address.getHex();
    }
}
