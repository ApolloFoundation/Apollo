/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.id.handler;

import com.apollocurrency.apl.id.cert.ApolloCertificate;
import java.math.BigInteger;

/**
 *
 * @author alukin@gmail.com
 */
public interface ThisNodeIdHandler {
    BigInteger getApolloId();
    ApolloCertificate getCertificate();
    byte[] sign(byte[] message);
}
