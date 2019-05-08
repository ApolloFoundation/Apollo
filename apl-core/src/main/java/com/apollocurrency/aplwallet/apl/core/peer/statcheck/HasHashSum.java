/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.statcheck;

import java.math.BigInteger;

/**
 *
 * @author alukin@gmail.com
 */
public interface HasHashSum {
    public BigInteger getHash();
    public String getId();
    default public boolean hasSameHash(HasHashSum other){
        return getHash().compareTo(other.getHash())==0;
    }
}
