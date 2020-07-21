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

    public GeneratorEntity(long accountId, byte[] keySeed, byte[] publicKey) {
        this.accountId = accountId;
        this.keySeed = keySeed;
        this.publicKey = publicKey;
    }

    public GeneratorEntity(byte[] keySeed) {
        this.keySeed = keySeed;
        this.publicKey = Crypto.getPublicKey(keySeed);
        this.accountId = AccountService.getId(publicKey);
/*
        globalSync.updateLock();
        try {
            if (blockchain.getHeight() >= blockchainConfig.getLastKnownBlock()) {
                setLastBlock(blockchain.getLastBlock());
            }
            sortedForgers = null;
        } finally {
            globalSync.updateUnlock();
        }
*/
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
