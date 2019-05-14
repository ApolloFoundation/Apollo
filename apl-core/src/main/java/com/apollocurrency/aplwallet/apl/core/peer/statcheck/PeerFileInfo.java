/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.statcheck;

import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import java.math.BigInteger;

/**
 *
 * @author alukin@gmail.com
 */
public class PeerFileInfo implements HasHashSum {

    private BigInteger hash;
    private String peerAddress;
    private final Peer peer;
    private final String fileId;

    public PeerFileInfo(Peer peer, String fileId) {
        this.peer = peer;
        this.fileId = fileId;
    }
    
    @Override
    public BigInteger getHash() {
        return hash;
    }

    @Override
    public String getId() {
        return peerAddress;
    }


    @Override
    public boolean retreiveHash() {
       hash = peer.retreiveHash(fileId);
       return hash!=null;
    }

}
