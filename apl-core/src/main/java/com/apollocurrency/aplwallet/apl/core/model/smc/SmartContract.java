/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model.smc;

import lombok.Data;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Data
public class SmartContract {
    private String address; //contract address/id
    private String data;//contract source code
    private String serializedObject;
    private String contractName;//constructor
    private String languageName;

    private BigInteger fuelValue;//initial fuel value
    private BigInteger fuelRemaining;
    private BigInteger fuelPrice;

    private String transactionId;

    private String method; //call constructor or method
    private String args;
    private String status; //contract status:
    private boolean disabled;

}
