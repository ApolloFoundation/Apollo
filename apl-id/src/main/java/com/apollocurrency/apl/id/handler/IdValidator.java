/*
 * Copyright © 2020 Apollo Foundation
 */

package com.apollocurrency.apl.id.handler;

import java.security.cert.X509Certificate;


/**
 *
 * @author alukin@gmail.com
 */
public interface IdValidator {
    void addTrustedSignerCert(X509Certificate  cert);
    boolean isSelfSigned(X509Certificate cert);
    boolean isTrusted(X509Certificate cert);    
}
