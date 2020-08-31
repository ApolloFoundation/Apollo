/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.id.handler;

import com.apollocurrency.apl.id.cert.ApolloCertificate;
import java.security.PrivateKey;
/**
 *
 * @author alukion@gmail.com
 */
public class CertKeyHolder {
    private final ApolloCertificate cert;
    private final PrivateKey privateKey;

    public CertKeyHolder(ApolloCertificate cert, PrivateKey privateKey) {
        this.cert = cert;
        this.privateKey = privateKey;
    }
    
}
