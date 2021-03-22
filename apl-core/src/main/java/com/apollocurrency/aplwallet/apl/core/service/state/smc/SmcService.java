/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.model.smc.SmartContract;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcService {
    void saveContract(SmartContract contract);

    SmartContract loadContract(String address);

    void updateContractState(SmartContract contract);

}
