/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.event;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractEventService;
import com.apollocurrency.smc.blockchain.event.ContractEventManagerFactory;
import com.apollocurrency.smc.contract.vm.ContractEvent;
import com.apollocurrency.smc.contract.vm.ContractEventManager;
import com.apollocurrency.smc.data.type.Address;

import javax.inject.Inject;

/**
 * @author andrew.zinchenko@gmail.com
 */

public class SmcContractEventManagerFactory implements ContractEventManagerFactory {
    private final SmcContractEventService contractEventService;

    @Inject
    public SmcContractEventManagerFactory(SmcContractEventService contractEventService) {
        this.contractEventService = contractEventService;
    }

    @Override
    public ContractEventManager create(final Address contract) {
        return new ContractEventManager() {
            @Override
            public void emit(ContractEvent event) {
                if (!contractEventService.isEventExist(contract, event.getEventType().getName())) {
                    //contractEventService.saveEventEntry();
                }
                //contractEventService.saveEventLogEntry();
            }
        };
    }

}
