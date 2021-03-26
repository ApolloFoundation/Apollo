/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */
package com.apollocurrency.aplwallet.apl.security.id;

import io.firstbridge.identity.handler.IdValidator;
import io.firstbridge.identity.handler.ThisActorIdHandler;


/**
 * Service that handles identity of peers using X.509 certificates od nodes
 * @author alukin@gmail.com
 */
public interface IdentityService {
    
    /**
     * Handler for this node ID
     * @return inited instance of ThisActorIdHandler
     */
    ThisActorIdHandler getThisNodeIdHandler();
    
    /**
     * Inied with set of trusted CA certifiates IdValidator instance
     * @return  Inied IdValidator instance
     */
    IdValidator getPeerIdValidator();
    
    /**
     * Load this node certificates and keys from defined directories;
     * Fails if certificate already exists but private key could not be loaded
     * If certificate/private key parit does not exist, self-signed ceretificate is generated.
     * @return true if key/certificate pair is loaded or generated and saved.
     * false if certificate exists but corresponding private key could not be loaded 
     * or generated cert and key could not be saved.
     */
    boolean loadMyIdentity();
    
    /**
     * Load set of trusted CA public certificates
     * @return 
     */
    boolean loadTrusterCaCerts();
    
}
