/*
 * Copyright (c)  2018-2021. Apollo Foundation.
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
     * Inied with set of trusted CA certificates IdValidator instance
     * @return  Inied IdValidator instance
     */
    IdValidator getPeerIdValidator();

    /**
     * Load this node certificates and keys from defined directories;
     * Fails if certificate already exists but private key could not be loaded
     * If certificate/private key pair does not exist, self-signed certificate is generated.
     * @return true if key/certificate pair is loaded or generated and saved.
     * false if certificate exists but corresponding private key could not be loaded
     * or generated cert and key could not be saved.
     */
    boolean loadMyIdentity();

    /**
     * Load set of trusted CA public certificates
     */
    boolean loadTrustedCaCerts();

}
