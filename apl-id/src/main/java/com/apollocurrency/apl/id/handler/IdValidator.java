/*
 * Copyright © 2020 Apollo Foundation
 */

package com.apollocurrency.apl.id.handler;

import com.apollocurrency.apl.id.cert.ApolloCertificate;

/**
 *
 * @author alukin@gmail.com
 */
public interface IdValidator {
    boolean isSelfSigned(ApolloCertificate cert);
    boolean isTrusted(ApolloCertificate cert);    
}
