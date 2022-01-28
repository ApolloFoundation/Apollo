/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import lombok.experimental.Delegate;

/**
 * The specified service to postpone the real modification of the data.
 * All changes will be committed later.
 *
 * @author andrew.zinchenko@gmail.com
 */
public abstract class PostponedContractService implements SmcContractRepository {
    @Delegate
    protected SmcContractRepository contractRepository;

    public PostponedContractService(SmcContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    /**
     * Fix all contract changes in the blockchain.
     * Saves new contract and updates state for existing contracts.
     */
    public abstract void commitContractChanges(Transaction transaction);

}
