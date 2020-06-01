/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.entity.operation.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.operation.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.operation.account.impl.AccountServiceImpl;

import javax.enterprise.inject.spi.CDI;
import java.math.BigInteger;

/**
 * Active generator
 */
public class ActiveGenerator implements Comparable<ActiveGenerator> {

    private static AccountService accountService;
    private final long accountId;
    private long hitTime;
    private long effectiveBalanceAPL;
    private byte[] publicKey;

    public ActiveGenerator(long accountId) {
        this.accountId = accountId;
        this.hitTime = Long.MAX_VALUE;
    }

    private AccountService lookupAccountService() {
        if (accountService == null) {
            accountService = CDI.current().select(AccountServiceImpl.class).get();
        }
        return accountService;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getEffectiveBalance() {
        return effectiveBalanceAPL;
    }

    public long getHitTime() {
        return hitTime;
    }

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
        BigInteger hit = Generator.getHit(publicKey, lastBlock);
        hitTime = Generator.getHitTime(effectiveBalance, hit, lastBlock);
    }

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
        return (hitTime < obj.hitTime ? -1 : (hitTime > obj.hitTime ? 1 : 0));
    }
}
