/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.event;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractEventService;
import com.apollocurrency.smc.blockchain.event.ContractEventManagerFactory;
import com.apollocurrency.smc.blockchain.event.LogInfoContractEventManager;
import com.apollocurrency.smc.contract.vm.ContractEventManager;
import com.apollocurrency.smc.data.type.Address;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class SmcContractEventManagerClassFactory {
    private static final ContractEventManagerFactory MOCK_EVENT_MANAGER_FACTORY = new MockContractEventManagerFactory();
    private final SmcContractEventService contractEventService;

    @Inject
    public SmcContractEventManagerClassFactory(SmcContractEventService contractEventService) {
        this.contractEventService = contractEventService;
    }

    public ContractEventManagerFactory createEventManagerFactory(Address transaction) {
        return new PersistentContractEventManagerFactory(transaction, contractEventService);
    }

    public ContractEventManagerFactory createMockEventManagerFactory() {
        return MOCK_EVENT_MANAGER_FACTORY;
    }

    private static class PersistentContractEventManagerFactory implements ContractEventManagerFactory {
        private final Address transaction;
        private final SmcContractEventService contractEventService;

        public PersistentContractEventManagerFactory(Address transaction, SmcContractEventService contractEventService) {
            this.transaction = transaction;
            this.contractEventService = contractEventService;
        }

        @Override
        public ContractEventManager create(Address contract) {
            return new SmcContractEventManager(contract, transaction, contractEventService);
        }
    }

    private static class MockContractEventManagerFactory implements ContractEventManagerFactory {
        @Override
        public ContractEventManager create(Address contract) {
            return new LogInfoContractEventManager();
        }
    }
}
