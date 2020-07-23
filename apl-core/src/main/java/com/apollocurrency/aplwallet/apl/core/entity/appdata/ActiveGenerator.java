/*
 *  Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.appdata;

/**
 * Active generator
 */
public class ActiveGenerator implements Comparable<ActiveGenerator> {

    private final long accountId;
    private long hitTime;
    private long effectiveBalanceAPL;
    private byte[] publicKey;

    public ActiveGenerator(long accountId) {
        this.accountId = accountId;
        this.hitTime = Long.MAX_VALUE;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getEffectiveBalance() {
        return effectiveBalanceAPL;
    }

    public void setEffectiveBalanceAPL(long effectiveBalanceAPL) {
        this.effectiveBalanceAPL = effectiveBalanceAPL;
    }

    public long getHitTime() {
        return hitTime;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public void setHitTime(long hitTime) {
        this.hitTime = hitTime;
    }

/*
    public void setLastBlock(Block lastBlock) {
        if (publicKey == null) {
            publicKey = lookupAccountService().getPublicKeyByteArray(accountId);
            if (publicKey == null) {
                hitTime = Long.MAX_VALUE;
                return;
            }
        }
        int height = lastBlock.getHeight();
        Account account = lookupAccountService().getAccount(accountId, height);
        if (account == null) {
            hitTime = Long.MAX_VALUE;
            return;
        }
        effectiveBalanceAPL = Math.max(lookupAccountService().getEffectiveBalanceAPL(account, height, true), 0);
        if (effectiveBalanceAPL == 0) {
            hitTime = Long.MAX_VALUE;
            return;
        }
        BigInteger effectiveBalance = BigInteger.valueOf(effectiveBalanceAPL);
        BigInteger hit = lookupGeneratorService().getHit(publicKey, lastBlock);
        hitTime = generatorService.getHitTime(effectiveBalance, hit, lastBlock);
    }
*/

    @Override
    public int hashCode() {
        return Long.hashCode(accountId);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null && (obj instanceof ActiveGenerator) && accountId == ((ActiveGenerator) obj).accountId);
    }

    @Override
    public int compareTo(ActiveGenerator obj) {
        return (Long.compare(hitTime, obj.hitTime));
    }
}
