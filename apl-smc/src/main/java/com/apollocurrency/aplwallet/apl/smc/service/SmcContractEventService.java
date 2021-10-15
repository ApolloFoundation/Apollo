/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service;

import com.apollocurrency.aplwallet.api.v2.model.ContractEventDetails;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractEvent;
import com.apollocurrency.smc.data.expr.Term;
import com.apollocurrency.smc.data.type.Address;

import java.util.List;


/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcContractEventService {
    /**
     * Checks if the given contract exist
     *
     * @param contract the given contract address
     * @return true if the given contract already stored into the blockchain
     */
    boolean isContractExist(Address contract);

    /**
     * Save the contract event info in the persistent storage
     *
     * @param event the emitted event
     */
    void saveEvent(AplContractEvent event);

    /**
     * Fire the CDI event
     *
     * @param event the nested contract event
     */
    void fireCdiEvent(AplContractEvent event);

    List<ContractEventDetails> getEventsByFilter(Long contract, String eventName, Term filter, int fromBlock, int toBlock, int from, int to, String order);
}
