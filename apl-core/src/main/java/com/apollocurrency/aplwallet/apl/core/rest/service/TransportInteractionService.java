/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.response.TransportStatusResponse;

public interface TransportInteractionService {

    /**
     * Get transport status
     *
     * @return TransportStatusResponse <i>null</i>
     */

    TransportStatusResponse getTransportStatusResponse();

    /**
     * Starting up with secure transport
     */

    void startSecureTransport();

    /**
     * Stopping secure transport
     */

    void stopSecureTransport();

    /**
     * Start transport interaction service
     */

    void start();

    /**
     * Start transport interaction service
     */

    void stop();

}
