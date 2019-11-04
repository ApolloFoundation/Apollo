/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.statcheck;

import lombok.Getter;
import lombok.Setter;
import org.spongycastle.util.Arrays;

/**
 * Entity that have hash and could be sorted by it
 * @author alukin@gmail.com
 */
public class PeerFileHashSum {
    @Getter
    @Setter
    private byte[] hash;
    @Getter    
    private final String peerId;
    @Getter
    private final String fileId;

    public PeerFileHashSum(String peerId, String fileId) {
        this.peerId = peerId;
        this.fileId = fileId;
    }

    public PeerFileHashSum(byte[] hash, String peerId, String fileId) {
        this.hash = hash;
        this.peerId = peerId;
        this.fileId = fileId;
    }
    
    public boolean hasSameHash(PeerFileHashSum other){
        if(other==null) return false;
        return Arrays.areEqual(getHash(),other.getHash());
    }
}
