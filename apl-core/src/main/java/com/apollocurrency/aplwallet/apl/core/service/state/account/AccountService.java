/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account;

import com.apollocurrency.aplwallet.apl.core.exception.DoubleSpendingException;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.apl.core.model.Balances;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountService {

    static long getId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return Convert.fullHashToId(publicKeyHash);
    }

    static void checkBalance(long accountId, long confirmed, long unconfirmed) {
        if (accountId == GenesisImporter.CREATOR_ID) {
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

    int getActiveLeaseCount();

    Account getAccount(long id);

    Account getAccount(long id, int height);

    Account getAccount(byte[] publicKey);

    Account getAccount(Account account);

    Account addGenesisAccount(long id);

    Account addOrGetAccount(long id);

    void update(Account account);

    List<Block> getAccountBlocks(long accountId, int from, int to, int timestamp);

    long getEffectiveBalanceAPL(Account account, int height, boolean lock);

    long getGuaranteedBalanceATM(Account account);

    long getGuaranteedBalanceATM(Account account, int numberOfConfirmations, int currentHeight);

    long getLessorsGuaranteedBalanceATM(Account account, int height);

    DbIterator<Account> getLessorsIterator(Account account);

    DbIterator<Account> getLessorsIterator(Account account, int height);

    List<Account> getLessors(Account account);

    List<Account> getLessors(Account account, int height);

    /**
     * Change the forged balance value and save into the data base.
     *
     * @param account   the account
     * @param amountATM the forged balance is increased on that value
     */
    void addToForgedBalanceATM(Account account, long amountATM);

    void addToBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM);

    void addToBalanceAndUnconfirmedBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM);

    void addToUnconfirmedBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM);

    void addToBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM);

    void addToBalanceAndUnconfirmedBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM);

    void addToUnconfirmedBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM);

    long getTotalAmountOnTopAccounts(int numberOfTopAccounts);

    long getTotalAmountOnTopAccounts();

    long getTotalNumberOfAccounts();

    List<Account> getTopHolders(int numberOfTopAccounts);

    long getTotalSupply();

    int getBlockchainHeight();

    Balances getAccountBalances(Account account, boolean includeEffectiveBalance);

    Balances getAccountBalances(Account account, boolean includeEffectiveBalance, int height);

    ApolloFbWallet generateUserAccounts(byte[] secretApl);

    //Delegated from  AccountPublicKeyService
    boolean setOrVerifyPublicKey(long accountId, byte[] key);

    byte[] getPublicKeyByteArray(long id);
}