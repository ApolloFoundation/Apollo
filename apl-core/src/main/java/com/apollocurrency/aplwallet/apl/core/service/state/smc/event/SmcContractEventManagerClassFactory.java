/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.event;

import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import com.apollocurrency.smc.blockchain.event.ContractEventManagerFactory;
import com.apollocurrency.smc.contract.vm.ContractEventManager;
import com.apollocurrency.smc.contract.vm.event.LogInfoContractEventManager;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.txlog.TxLog;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class SmcContractEventManagerClassFactory {
    private static final ContractEventManagerFactory MOCK_EVENT_MANAGER_FACTORY = new MockContractEventManagerFactory();
    private final HashSumProvider hashSumProvider;

    @Inject
    public SmcContractEventManagerClassFactory(HashSumProvider hashSumProvider) {
        this.hashSumProvider = hashSumProvider;
    }

    public ContractEventManagerFactory createEventManagerFactory(Address transaction, int height, TxLog txLog) {
        return contract -> new AplContractEventManager(contract, transaction, height, hashSumProvider, txLog);
    }

    public ContractEventManagerFactory createMockEventManagerFactory() {
        return MOCK_EVENT_MANAGER_FACTORY;
    }

    private static class MockContractEventManagerFactory implements ContractEventManagerFactory {
        @Override
        public ContractEventManager create(Address contract) {
            return new LogInfoContractEventManager();
        }
    }
}
