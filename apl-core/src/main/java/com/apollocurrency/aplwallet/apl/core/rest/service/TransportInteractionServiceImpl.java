package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.response.TransportStatusResponse;
import javax.inject.Singleton;


@Singleton
public class TransportInteractionServiceImpl implements TransportInteractionService {
    

    @Override
    public TransportStatusResponse getTransportStatusResponse() {
        return new TransportStatusResponse();
    }

    
}