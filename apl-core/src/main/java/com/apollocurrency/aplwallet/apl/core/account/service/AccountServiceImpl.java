/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.*;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.app.*;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.*;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;

/**
 * @author andrew.zinchenko@gmail.com
 */

@Slf4j
@Singleton
public class AccountServiceImpl implements AccountService {

    @Inject @Setter
    private AccountTable accountTable;

    @Inject @Setter
    private Blockchain blockchain;

    @Inject @Setter
    private BlockchainConfig blockchainConfig;

    @Inject @Setter
    private GlobalSync sync;

    @Inject @Setter
    private DatabaseManager databaseManager;

    @Inject @Setter
    private AccountPublicKeyService accountPublicKeyService;

    @Inject @Setter
    private Event<Account> accountEvent;

    @Override
    public int getActiveLeaseCount() {
        return accountTable.getCount(new DbClause.NotNullClause("active_lessee_id"));
    }

    private BlockchainProcessor blockchainProcessor;
    //TODO this lookup-method prevents the cyclic dependencies, need to be removed after refactoring the BlockchainProcessor class
    private void lookupAndInjectBlockchainProcessor() {
        if (this.blockchainProcessor == null) {
            this.blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        }
    }
    //TODO this lookup-method prevents the cyclic dependencies, need to be removed after refactoring the BlockchainProcessor class
    private AccountLedgerService accountLedgerService;
    private AccountLedgerService lookupAccountLedgerService() {
        if (this.accountLedgerService == null) {
            this.accountLedgerService = CDI.current().select(AccountLedgerServiceImpl.class).get();
        }
        return accountLedgerService;
    }

    @Override
    public Account getAccount(long id) {
        DbKey dbKey = AccountTable.newKey(id);
        Account account = accountTable.get(dbKey);
        if (account == null) {
            PublicKey publicKey = accountPublicKeyService.getPublicKey(dbKey);
            if (publicKey != null) {
                account = accountTable.newEntity(dbKey);
                account.setPublicKey(publicKey);
            }
        }
        return account;
    }

    @Override
    public Account getAccount(long id, int height) {
        DbKey dbKey = AccountTable.newKey(id);
        Account account = accountTable.get(dbKey, height);
        if (account == null) {
            PublicKey publicKey = accountPublicKeyService.getPublicKey(dbKey, height);
            if (publicKey != null) {
                account = new Account(id, height);
                account.setPublicKey(publicKey);
            }
        }
        return account;
    }

    @Override
    public Account getAccount(byte[] publicKey) {
        long accountId = AccountService.getId(publicKey);
        Account account = getAccount(accountId);
        if (account == null) {
            return null;
        }
        if (account.getPublicKey() == null) {
            account.setPublicKey(accountPublicKeyService.getPublicKey(AccountTable.newKey(account)));
        }
        if (account.getPublicKey() == null || account.getPublicKey().getPublicKey() == null
                || Arrays.equals(account.getPublicKey().getPublicKey(), publicKey)) {
            return account;
        }
        throw new RuntimeException("DUPLICATE KEY for account " + Long.toUnsignedString(accountId)
                + " existing key " + Convert.toHexString(account.getPublicKey().getPublicKey()) + " new key " + Convert.toHexString(publicKey));
    }

    @Override
    public Account addOrGetAccount(long id) {
        return addOrGetAccount(id, false);
    }

    @Override
    public Account addOrGetAccount(long id, boolean isGenesis) {
        if (id == 0) {
            throw new IllegalArgumentException("Invalid accountId 0");
        }
        DbKey dbKey = AccountTable.newKey(id);
        Account account = accountTable.get(dbKey);
        if (account == null) {
            account = accountTable.newEntity(dbKey);
            PublicKey publicKey = accountPublicKeyService.getPublicKey(dbKey);
            if (publicKey == null) {
                publicKey = accountPublicKeyService.insertNewPublicKey(dbKey, isGenesis);
            }
            account.setPublicKey(publicKey);
        }
        return account;
    }

    @Override
    public void save(Account account) {
        if (account.getBalanceATM() == 0
                && account.getUnconfirmedBalanceATM() == 0
                && account.getForgedBalanceATM() == 0
                && account.getActiveLesseeId() == 0
                && account.getControls().isEmpty()) {
            accountTable.delete(account);
        } else {
            accountTable.insert(account);
        }
    }

    @Override
    public long getEffectiveBalanceAPL(Account account, int height, boolean lock) {
        if (height <= 1440) {
            Account genesisAccount = getAccount(account.getId(), 0);
            return genesisAccount == null ? 0 : genesisAccount.getBalanceATM() / Constants.ONE_APL;
        }
        if (account.getPublicKey() == null) {
            account.setPublicKey(accountPublicKeyService.getPublicKey(AccountTable.newKey(account.getId())));
        }
        if (account.getPublicKey() == null || account.getPublicKey().getPublicKey() == null || height - account.getPublicKey().getHeight() <= 1440) {
            return 0; // cfb: Accounts with the public key revealed less than 1440 blocks ago are not allowed to generate blocks
        }
        if (lock) {
            sync.readLock();
        }
        try {
            long effectiveBalanceATM = getLessorsGuaranteedBalanceATM(account, height);
            if (account.getActiveLesseeId() == 0) {
                effectiveBalanceATM += getGuaranteedBalanceATM(account, blockchainConfig.getGuaranteedBalanceConfirmations(), height);
            }
            return effectiveBalanceATM < Constants.MIN_FORGING_BALANCE_ATM ? 0 : effectiveBalanceATM / Constants.ONE_APL;
        }
        finally {
            if (lock) {
                sync.readUnlock();
            }
        }
    }

    @Override
    public long getGuaranteedBalanceATM(Account account) {
        return getGuaranteedBalanceATM(account, blockchainConfig.getGuaranteedBalanceConfirmations(), blockchain.getHeight());
    }

    @Override
    public long getGuaranteedBalanceATM(Account account, final int numberOfConfirmations, final int currentHeight) {
        lookupAndInjectBlockchainProcessor();
        sync.readLock();
        try {
            int height = currentHeight - numberOfConfirmations;
            if (height + blockchainConfig.getGuaranteedBalanceConfirmations() < blockchainProcessor.getMinRollbackHeight()
                    || height > blockchain.getHeight()) {
                throw new IllegalArgumentException("Height " + height + " not available for guaranteed balance calculation");
            }
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            try (Connection con = dataSource.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT SUM (additions) AS additions "
                         + "FROM account_guaranteed_balance WHERE account_id = ? AND height > ? AND height <= ?")) {
                pstmt.setLong(1, account.getId());
                pstmt.setInt(2, height);
                pstmt.setInt(3, currentHeight);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        return account.getBalanceATM();
                    }
                    return Math.max(Math.subtractExact(account.getBalanceATM(), rs.getLong("additions")), 0);
                }
            }
            catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
        finally {
            sync.readUnlock();
        }
    }

    @Override
    public long getLessorsGuaranteedBalanceATM(Account account, int height) {
        List<Account> lessors = getLessors(account, height);
        Long[] lessorIds = new Long[lessors.size()];
        long[] balances = new long[lessors.size()];
        for (int i = 0; i < lessors.size(); i++) {
            lessorIds[i] = lessors.get(i).getId();
            balances[i] = lessors.get(i).getBalanceATM();
        }

        int blockchainHeight = blockchain.getHeight();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT account_id, SUM (additions) AS additions "
                     + "FROM account_guaranteed_balance, TABLE (id BIGINT=?) T WHERE account_id = T.id AND height > ? "
                     + (height < blockchainHeight ? " AND height <= ? " : "")
                     + " GROUP BY account_id ORDER BY account_id")) {
            pstmt.setObject(1, lessorIds);
            pstmt.setInt(2, height - blockchainConfig.getGuaranteedBalanceConfirmations());
            if (height < blockchainHeight) {
                pstmt.setInt(3, height);
            }
            long total = 0;
            int i = 0;
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long accountId = rs.getLong("account_id");
                    while (lessorIds[i] < accountId && i < lessorIds.length) {
                        total += balances[i++];
                    }
                    if (lessorIds[i] == accountId) {
                        total += Math.max(balances[i++] - rs.getLong("additions"), 0);
                    }
                }
            }
            while (i < balances.length) {
                total += balances[i++];
            }
            return total;
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Account> getLessorsIterator(Account account) {
        DbIterator<Account> iterator = accountTable.getManyBy(new DbClause.LongClause("active_lessee_id", account.getId()), 0, -1, " ORDER BY id ASC ");
        return iterator;
    }

    @Override
    public DbIterator<Account> getLessorsIterator(Account account, int height) {
        DbIterator<Account> iterator = accountTable.getManyBy(new DbClause.LongClause("active_lessee_id", account.getId()), height, 0, -1, " ORDER BY id ASC ");
        return iterator;
    }


    @Override
    public List<Account> getLessors(Account account) {
        List<Account> result = new ArrayList<>();
        try(DbIterator<Account> iterator = getLessorsIterator(account)) {
            iterator.forEachRemaining(result::add);
        }
        return result;
    }

    @Override
    public List<Account> getLessors(Account account, int height) {
        List<Account> result = new ArrayList<>();
        try(DbIterator<Account> iterator = getLessorsIterator(account, height)) {
            iterator.forEachRemaining(result::add);
        }
        return result;
    }

    private void logEntryConfirmed(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (lookupAccountLedgerService().mustLogEntry(account.getId(), false)) {
            if (feeATM != 0) {
                lookupAccountLedgerService().logEntry(new LedgerEntry(LedgerEvent.TRANSACTION_FEE, eventId, account.getId(),
                        LedgerHolding.APL_BALANCE, null, feeATM, account.getBalanceATM() - amountATM, blockchain.getLastBlock()));
            }
            if (amountATM != 0) {
                lookupAccountLedgerService().logEntry(new LedgerEntry(event, eventId, account.getId(),
                        LedgerHolding.APL_BALANCE, null, amountATM, account.getBalanceATM(), blockchain.getLastBlock()));
            }
        }
    }

    private void logEntryUnconfirmed(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (lookupAccountLedgerService().mustLogEntry(account.getId(), true)) {
            if (feeATM != 0) {
                lookupAccountLedgerService().logEntry(new LedgerEntry(LedgerEvent.TRANSACTION_FEE, eventId, account.getId(),
                        LedgerHolding.UNCONFIRMED_APL_BALANCE, null, feeATM, account.getUnconfirmedBalanceATM() - amountATM, blockchain.getLastBlock()));
            }
            if (amountATM != 0) {
                lookupAccountLedgerService().logEntry(new LedgerEntry(event, eventId, account.getId(),
                        LedgerHolding.UNCONFIRMED_APL_BALANCE, null, amountATM, account.getUnconfirmedBalanceATM(), blockchain.getLastBlock()));
            }
        }
    }

    @Override
    public void addToBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (amountATM == 0 && feeATM == 0) {
            return;
        }
        if (feeATM != 0){
            log.trace("Add c balance for {} from {} , amount - {}, total conf- {}, height- {}",
                    account.getId(), Thread.currentThread().getStackTrace()[2].getMethodName(),
                    amountATM, amountATM + account.getBalanceATM(), blockchain.getHeight());
        }
        long totalAmountATM = Math.addExact(amountATM, feeATM);
        account.setBalanceATM(Math.addExact(account.getBalanceATM(), totalAmountATM));
        addToGuaranteedBalanceATM(account, totalAmountATM);
        AccountService.checkBalance(account.getId(), account.getBalanceATM(), account.getUnconfirmedBalanceATM());
        save(account);

        accountEvent.select(literal(AccountEventType.BALANCE)).fire(account);
        logEntryConfirmed(account, event, eventId, amountATM, feeATM);
    }

    @Override
    public  void addToBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM) {
        addToBalanceATM(account, event, eventId, amountATM, 0);
        log.trace("Add c balance for {} from {} , amount - {}, total conf- {}, height -{}", account.getId(), Thread.currentThread().getStackTrace()[2].getMethodName(), amountATM, amountATM + account.getBalanceATM(), blockchain.getHeight());
    }

    @Override
    public void addToBalanceAndUnconfirmedBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (amountATM == 0 && feeATM == 0) {
            return;
        }
        if (feeATM != 0){
            log.trace("Add u balance for {} from {} , amount - {}, total unc {}, height - {}",
                    account.getId(), Thread.currentThread().getStackTrace()[2].getMethodName(),
                    amountATM, amountATM + account.getUnconfirmedBalanceATM(), blockchain.getHeight());
        }
        long totalAmountATM = Math.addExact(amountATM, feeATM);
        account.setBalanceATM(Math.addExact(account.getBalanceATM(), totalAmountATM));
        account.setUnconfirmedBalanceATM(Math.addExact(account.getUnconfirmedBalanceATM(), totalAmountATM));
        addToGuaranteedBalanceATM(account, totalAmountATM);
        AccountService.checkBalance(account.getId(), account.getBalanceATM(), account.getUnconfirmedBalanceATM());
        save(account);

        accountEvent.select(literal(AccountEventType.BALANCE)).fire(account);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_BALANCE)).fire(account);

        if (event == null) {
            return;
        }
        logEntryUnconfirmed(account, event, eventId, amountATM, feeATM);
        logEntryConfirmed(account, event, eventId, amountATM, feeATM);
    }

    @Override
    public void addToBalanceAndUnconfirmedBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM) {
        addToBalanceAndUnconfirmedBalanceATM(account, event, eventId, amountATM, 0);
        log.trace("Add c and  u balance for {} from {} , amount - {}, total conf- {}, total unc {}, height {}",
                account.getId(), Thread.currentThread().getStackTrace()[2].getMethodName(),
                amountATM, amountATM + account.getBalanceATM(), amountATM + account.getUnconfirmedBalanceATM(), blockchain.getHeight());
    }

    private void addToGuaranteedBalanceATM(Account account, long amountATM) {
        if (amountATM <= 0) {
            return;
        }
        int blockchainHeight = blockchain.getHeight();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT additions FROM account_guaranteed_balance "
                     + "WHERE account_id = ? and height = ?");
             PreparedStatement pstmtUpdate = con.prepareStatement("MERGE INTO account_guaranteed_balance (account_id, "
                     + " additions, height) KEY (account_id, height) VALUES(?, ?, ?)")) {
            pstmtSelect.setLong(1, account.getId());
            pstmtSelect.setInt(2, blockchainHeight);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                long additions = amountATM;
                if (rs.next()) {
                    additions = Math.addExact(additions, rs.getLong("additions"));
                }
                pstmtUpdate.setLong(1, account.getId());
                pstmtUpdate.setLong(2, additions);
                pstmtUpdate.setInt(3, blockchainHeight);
                pstmtUpdate.executeUpdate();
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void addToUnconfirmedBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (amountATM == 0 && feeATM == 0) {
            return;
        }
        if (feeATM!=0){
            log.trace("Add u balance for {} from {} , amount - {}, total unc {}, height - {}",
                    account.getId(), Thread.currentThread().getStackTrace()[2].getMethodName(),
                    amountATM, amountATM + account.getUnconfirmedBalanceATM(), blockchain.getHeight());
        }
        long totalAmountATM = Math.addExact(amountATM, feeATM);
        account.setUnconfirmedBalanceATM(Math.addExact(account.getUnconfirmedBalanceATM(), totalAmountATM));
        AccountService.checkBalance(account.getId(), account.getBalanceATM(), account.getUnconfirmedBalanceATM());
        save(account);

        accountEvent.select(literal(AccountEventType.UNCONFIRMED_BALANCE)).fire(account);

        if (event == null) {
            return;
        }
        logEntryUnconfirmed(account, event, eventId, amountATM, feeATM);
    }

    @Override
    public void addToUnconfirmedBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM) {
        addToUnconfirmedBalanceATM(account, event, eventId, amountATM, 0);
        log.trace("Add u balance for {} from {} , amount - {}, total unc {}, height - {}",
                account.getId(), Thread.currentThread().getStackTrace()[2].getMethodName(),
                amountATM, amountATM + account.getUnconfirmedBalanceATM(), blockchain.getHeight());
    }

    @Override
    public long getTotalAmountOnTopAccounts(int numberOfTopAccounts) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try(Connection con = dataSource.getConnection()) {
            return AccountTable.getTotalAmountOnTopAccounts(con, numberOfTopAccounts);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public long getTotalAmountOnTopAccounts() {
        return getTotalAmountOnTopAccounts(100);
    }


    @Override
    public long getTotalNumberOfAccounts() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try(Connection con = dataSource.getConnection()) {
            return AccountTable.getTotalNumberOfAccounts(con);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Account> getTopHolders(Connection con, int numberOfTopAccounts) {
        try {
            return accountTable.getTopHolders(con, numberOfTopAccounts);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public long getTotalSupply() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try(Connection con = dataSource.getConnection()) {
            return AccountTable.getTotalSupply(con);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getBlockchainHeight() {
        return blockchain.getHeight();
    }

    //Delegated from  AccountPublicKeyService
    @Override
    public boolean setOrVerify(long accountId, byte[] key) {
        return accountPublicKeyService.setOrVerify(accountId, key);
    }

    @Override
    public byte[] getPublicKey(long id) {
        return accountPublicKeyService.getPublicKey(id);
    }
}

