/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.model.smc.AplContractEvent;


/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcContractEventService {

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
}
