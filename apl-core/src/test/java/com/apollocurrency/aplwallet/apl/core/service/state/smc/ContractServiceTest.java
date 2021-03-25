/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToStateEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractStateTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractTable;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.ContractServiceImpl;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import com.apollocurrency.smc.contract.SmartContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    Blockchain blockchain;
    @Mock
    SmcContractTable smcContractTable;
    @Mock
    SmcContractStateTable smcContractStateTable;
    @Mock
    ContractModelToEntityConverter contractModelToEntityConverter;
    @Mock
    ContractModelToStateEntityConverter contractModelToStateConverter;
    @Mock
    HashSumProvider hashSumProvider;

    ContractService contractService;

    @BeforeEach
    void setUp() {
        initMocks(this);

        contractService = new ContractServiceImpl(blockchain,
            smcContractTable,
            smcContractStateTable,
            contractModelToEntityConverter,
            contractModelToStateConverter,
            hashSumProvider);

    }

    @Test
    void saveContract() {
        //GIVEN
        SmartContract =


            //WHEN
            contractService.saveContract();

        //THEN

    }

    @Test
    void loadContract() {

    }

    @Test
    void updateContractState() {

    }

    @Test
    void loadSerializedContract() {

    }

    @Test
    void saveSerializedContract() {

    }

    @Test
    void createNewContract() {

    }
}
