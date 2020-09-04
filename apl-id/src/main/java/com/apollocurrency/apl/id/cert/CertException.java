package com.apollocurrency.apl.id.cert;

/**
 * @author alukin@gmail.com
 */
public class CertException extends RuntimeException {

    CertException(String message) {
        throw new RuntimeException(message);
    }

}
