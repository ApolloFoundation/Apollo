/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.statcheck;

import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerAddress;
import java.math.BigInteger;

/**
 *
 * @author alukin@gmail.com
 */
public class PeerFileInfo implements HasHashSum {

    BigInteger hash;
    String peerAddress;
    Peer peer = null;

    @Override
    public BigInteger getHash() {
        return hash;
    }

    @Override
    public String getId() {
        return peerAddress;
    }

    public void setPeer(Peer p) {
        peerAddress = p.getHost();
    }

}
