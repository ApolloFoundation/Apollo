/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.appdata;

import java.math.BigInteger;

import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class GeneratorEntity implements Comparable<GeneratorEntity> {

    private final long accountId;
    private final byte[] keySeed;
    private final byte[] publicKey;
    private volatile long hitTime;
    private volatile BigInteger hit;
    @Getter
    private volatile BigInteger effectiveBalance;
    private volatile long deadline;

        public GeneratorEntity(byte[] keySeed, byte[] publicKey, long accountId) {
        this.keySeed = keySeed;
        this.publicKey = publicKey;
        this.accountId = accountId;
    }

    public int getTimestamp(int generationLimit) {
        return (generationLimit - hitTime > 3600) ? generationLimit : (int) hitTime + 1;
    }

    @Override
    public int compareTo(GeneratorEntity g) {
        int i = this.hit.multiply(g.effectiveBalance).compareTo(g.hit.multiply(this.effectiveBalance));
        if (i != 0) {
            return i;
        }
        return Long.compare(accountId, g.accountId);
    }

}
