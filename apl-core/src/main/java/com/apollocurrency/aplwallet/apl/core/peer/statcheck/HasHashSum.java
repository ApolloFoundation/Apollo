/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.statcheck;

import java.math.BigInteger;
import org.spongycastle.util.Arrays;

/**
 * Entity that have hash and could be sorted by it
 * @author alukin@gmail.com
 */
public interface HasHashSum {
    /**
     * Get already retreived hash
     * @return hash bytes
     */
    public byte[] getHash();
    public String getId();
    default public boolean hasSameHash(HasHashSum other){
        if(other==null) return false;
        return Arrays.areEqual(getHash(),other.getHash());
    }
    /**
     * get hash rom remote or other subsystem
     * @return hash bytes
     */
    public byte[] retreiveHash();
}
