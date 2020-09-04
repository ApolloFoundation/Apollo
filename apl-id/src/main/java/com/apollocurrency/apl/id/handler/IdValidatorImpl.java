/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.id.handler;

import com.apollocurrency.apl.id.cert.CertHelper;
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
    public boolean isSelfSigned(X509Certificate cert) {
        return false;
    }
    

    @Override
    public boolean isTrusted(X509Certificate cert) {
        boolean res = false;
        CertHelper ac = new CertHelper(cert);
        for(X509Certificate signerCert: trustedSigners){
            res = ac.isSignedBy(signerCert);
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
