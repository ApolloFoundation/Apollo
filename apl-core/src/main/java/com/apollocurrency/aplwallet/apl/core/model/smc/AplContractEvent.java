/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model.smc;

import com.apollocurrency.smc.data.type.ContractEvent;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Delegate;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public class AplContractEvent {
    private long id;
    private long contractId;
    private long transactionId;

    @Delegate
    private ContractEvent event;

}
