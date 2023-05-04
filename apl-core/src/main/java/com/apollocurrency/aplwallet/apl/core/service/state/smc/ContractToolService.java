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

    boolean validateContractSource(String source);

    SmartSource createSmartSource(SmcPublishContractAttachment attachment);

    SmartSource createSmartSource(String name, String source, String languageName);

    SmartSource completeContractSource(SmartSource smartSource);

    /**
     * Creates a new smart contract instance. That one is not persisted in the blockchain.
     * The address of the smart contract is a transaction id.
     *
     * @param smcTransaction blockchain transaction instance
     * @return smart contract instance
     */
    SmartContract createNewContract(Transaction smcTransaction);

    /**
     * Creates a mock smart contract instance. That one is not persisted in the blockchain.
     * This address of this smart contract instance is ZERO address.
     * This one used for contract source code validation.
     *
     * @param smcTransaction blockchain transaction instance
     * @return smart contract instance
     */
    SmartContract createMockContract(Transaction smcTransaction);

}
