/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model.smc;

import com.apollocurrency.smc.data.type.Address;
import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;
import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Data
@Builder
public class AplSmcContract {
    private Address address; //contract address/id
    private String data;//contract source code
    private String serializedObject;
    private String contractName;//constructor
    private String languageName;

    private BigInteger fuelValue;//initial fuel value
    private BigInteger fuelRemaining;
    private BigInteger fuelPrice;

    private long transactionId;

    private String method; //call constructor or method
    private List<String> args;
    private String status; //contract status:
    private boolean disabled;

}
