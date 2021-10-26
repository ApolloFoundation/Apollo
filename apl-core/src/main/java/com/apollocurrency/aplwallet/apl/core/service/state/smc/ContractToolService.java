/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.polyglot.language.SmartSource;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface ContractToolService {

    boolean validateContractSource(SmartSource source);

    SmartSource createSmartSource(SmcPublishContractAttachment attachment);

    /**
     * Creates a new smart contract instance. That one is not persisted in the blockchain.
     *
     * @param smcTransaction blockchain transaction instance
     * @return smart contract instance
     */
    SmartContract createNewContract(Transaction smcTransaction);

}
