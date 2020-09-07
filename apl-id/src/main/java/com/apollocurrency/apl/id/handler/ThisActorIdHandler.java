/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.id.handler;

import com.apollocurrency.apl.id.cert.ExtCert;
import java.math.BigInteger;
import java.nio.file.Path;

/**
 * Handles operations with X509 certificate and private key of this node 
 * @author alukin@gmail.com
 */
public interface ThisActorIdHandler {
    BigInteger getActorId();
    ExtCert getCertHelper();
    byte[] sign(byte[] message);
    void generateSelfSignedCert();
    boolean loadCertAndKey(Path baseDir);
    boolean saveAll(Path baseDir);
}
