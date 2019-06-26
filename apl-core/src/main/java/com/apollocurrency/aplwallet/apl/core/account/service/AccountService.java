/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEvent;
import com.apollocurrency.aplwallet.apl.core.account.DoubleSpendingException;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.app.Genesis;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;

import java.sql.Connection;
import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountService {
    Listeners<AccountEntity, AccountEvent> listeners = new Listeners<>();

    static boolean addListener(Listener<AccountEntity> listener, AccountEvent eventType) {
        return listeners.addListener(listener, eventType);
    }

    static boolean removeListener(Listener<AccountEntity> listener, AccountEvent eventType) {
        return listeners.removeListener(listener, eventType);
    }

    static long getId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return Convert.fullHashToId(publicKeyHash);
    }

    int getActiveLeaseCount();

    AccountEntity getAccountEntity(long id);

    AccountEntity getAccountEntity(long id, int height);

    AccountEntity getAccountEntity(byte[] publicKey);

    AccountEntity addOrGetAccount(long id);

    AccountEntity addOrGetAccount(long id, boolean isGenesis);

    void save(AccountEntity account);

    long getEffectiveBalanceAPL(AccountEntity account, int height, boolean lock);

    long getGuaranteedBalanceATM(AccountEntity account);

    long getGuaranteedBalanceATM(AccountEntity account, int numberOfConfirmations, int currentHeight);

    long getLessorsGuaranteedBalanceATM(AccountEntity account, int height);

    DbIterator<AccountEntity> getLessorsIterator(AccountEntity account);

    DbIterator<AccountEntity> getLessorsIterator(AccountEntity account, int height);

    List<AccountEntity> getLessors(AccountEntity account);

    List<AccountEntity> getLessors(AccountEntity account, int height);

    static void checkBalance(long accountId, long confirmed, long unconfirmed) {
        if (accountId == Genesis.CREATOR_ID) {
            return;
        }
        if (confirmed < 0) {
            throw new DoubleSpendingException("Negative balance or quantity: ", accountId, confirmed, unconfirmed);
        }
        if (unconfirmed < 0) {
            throw new DoubleSpendingException("Negative unconfirmed balance or quantity: ", accountId, confirmed, unconfirmed);
        }
        if (unconfirmed > confirmed) {
            throw new DoubleSpendingException("Unconfirmed exceeds confirmed balance or quantity: ", accountId, confirmed, unconfirmed);
        }
    }

    void addToBalanceATM(AccountEntity account, LedgerEvent event, long eventId, long amountATM, long feeATM);

    void addToBalanceAndUnconfirmedBalanceATM(AccountEntity account, LedgerEvent event, long eventId, long amountATM);

    void addToUnconfirmedBalanceATM(AccountEntity account, LedgerEvent event, long eventId, long amountATM, long feeATM);

    void addToBalanceATM(AccountEntity account, LedgerEvent event, long eventId, long amountATM);

    void addToBalanceAndUnconfirmedBalanceATM(AccountEntity account, LedgerEvent event, long eventId, long amountATM, long feeATM);

    void addToUnconfirmedBalanceATM(AccountEntity account, LedgerEvent event, long eventId, long amountATM);

    long getTotalAmountOnTopAccounts(int numberOfTopAccounts);

    long getTotalAmountOnTopAccounts();

    long getTotalNumberOfAccounts();

    DbIterator<AccountEntity> getTopHolders(Connection con, int numberOfTopAccounts);

    long getTotalSupply();

    //Delegated from  AccountPublicKeyService
    boolean setOrVerify(long accountId, byte[] key);
    byte[] getPublicKey(long id);
}
