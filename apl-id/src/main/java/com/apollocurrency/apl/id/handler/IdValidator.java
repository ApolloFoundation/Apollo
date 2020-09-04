/*
 * Copyright © 2020 Apollo Foundation
 */

package com.apollocurrency.apl.id.handler;

import com.apollocurrency.apl.id.cert.ApolloCertificate;
import java.security.cert.X509Certificate;


/**
 *
 * @author alukin@gmail.com
 */
public interface IdValidator {
    void addTrustedSignerCert(X509Certificate  cert);
    boolean isSelfSigned(ApolloCertificate cert);
    boolean isTrusted(ApolloCertificate cert);    
}
