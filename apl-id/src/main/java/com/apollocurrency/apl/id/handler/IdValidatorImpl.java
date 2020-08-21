/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.id.handler;

import com.apollocurrency.apl.id.cert.ApolloCertificate;

/**
 *
 * @author alukin@gmail.com
 */
public class IdValidatorImpl implements IdValidator{

    @Override
    public boolean isSelfSigned(ApolloCertificate cert) {
        return false;
    }

    @Override
    public boolean isTrusted(ApolloCertificate cert) {
        return true;
    }
    
}
