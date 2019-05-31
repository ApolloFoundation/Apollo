/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.statcheck;

import java.math.BigInteger;

/**
 * Entity that have hash and could be sorted by it
 * @author alukin@gmail.com
 */
public interface HasHashSum {
    /**
     * Get already retreived hash
     * @return hash bytes
     */
    public BigInteger getHash();
    public String getId();
    default public boolean hasSameHash(HasHashSum other){
        return getHash().compareTo(other.getHash())==0;
    }
    /**
     * get hash rom remote or other subsystem
     * @return hash byets
     */
    public BigInteger retreiveHash();
}
