/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.model;

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
public class AplContractEvent implements ContractEvent {
    private long id;

    @Delegate
    private ContractEvent event;

}
