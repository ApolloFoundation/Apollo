/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.smc.data.type.Address;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketException;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class ContractNotFoundException extends WebSocketException {
    private static final String MSG = "Contract not found, address=";

    public ContractNotFoundException(Address contract) {
        super(MSG + contract);
    }

    public ContractNotFoundException(Address contract, Throwable cause) {
        super(MSG + contract, cause);
    }
}
