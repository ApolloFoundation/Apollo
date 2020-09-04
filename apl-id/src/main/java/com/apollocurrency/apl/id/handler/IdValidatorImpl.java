/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.id.handler;

import com.apollocurrency.apl.id.cert.ApolloCertificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author alukin@gmail.com
 */
public class IdValidatorImpl implements IdValidator{
    
    private final List<X509Certificate> trustedSigners = new ArrayList<>();
    
    @Override
    public boolean isSelfSigned(ApolloCertificate cert) {
        return false;
    }

    @Override
    public boolean isTrusted(ApolloCertificate cert) {
        boolean res = false;
        for(X509Certificate signerCert: trustedSigners){
            res = cert.isSignedBy(signerCert);
            if(res){
                break;
            }
        }
        return res;
    }

    @Override
    public void addTrustedSignerCert(X509Certificate cert) {
        trustedSigners.add(cert);
    }
    
}
