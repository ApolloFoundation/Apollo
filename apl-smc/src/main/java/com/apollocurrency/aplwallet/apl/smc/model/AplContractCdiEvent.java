/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.model;

import com.apollocurrency.smc.contract.vm.event.NamedParameters;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public class AplContractCdiEvent {
    private long id;
    private long contractId;
    private long transactionId;
    private int txIdx;//the sequential number within the transaction

    private String spec;
    private byte[] signature;
    private String name;
    private NamedParameters params;
}
