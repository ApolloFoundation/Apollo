/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

/**
 * @author alukin@gmail.com
 */
public class PeerNotConnectedException extends Exception {

    /**
     * Creates a new instance of <code>PeerNotConnectedException</code> without
     * detail message.
     */
    public PeerNotConnectedException() {
    }

    /**
     * Constructs an instance of <code>PeerNotConnectedException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public PeerNotConnectedException(String msg) {
        super(msg);
    }
}
