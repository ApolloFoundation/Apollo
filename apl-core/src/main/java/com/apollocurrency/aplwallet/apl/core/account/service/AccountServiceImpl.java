/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.LedgerHolding;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;
import static com.apollocurrency.aplwallet.apl.core.app.CollectionUtil.toList;
import static com.apollocurrency.aplwallet.apl.util.ThreadUtils.last3Stacktrace;

/**
 * @author andrew.zinchenko@gmail.com
 */

@Slf4j
@Singleton
public class AccountServiceImpl implements AccountService {

    public static final int EFFECTIVE_BALANCE_CONFIRMATIONS = 1440;

    private final AccountTable accountTable;
    private final AccountGuaranteedBalanceTable accountGuaranteedBalanceTable;
    private final Blockchain blockchain;
    private final BlockchainConfig blockchainConfig;
    private final GlobalSync sync;
    private final AccountPublicKeyService accountPublicKeyService;
    private final Event<Account> accountEvent;
    private final Event<LedgerEntry> logLedgerEvent;
    private BlockchainProcessor blockchainProcessor;

    @Inject
    public AccountServiceImpl(AccountTable accountTable, Blockchain blockchain, BlockchainConfig blockchainConfig,
                              GlobalSync sync,
                              AccountPublicKeyService accountPublicKeyService,
                              Event<Account> accountEvent, Event<LedgerEntry> logLedgerEvent,
                              AccountGuaranteedBalanceTable accountGuaranteedBalanceTable) {
        this.accountTable = accountTable;
        this.blockchain = blockchain;
        this.blockchainConfig = blockchainConfig;
        this.sync = sync;
        this.accountPublicKeyService = accountPublicKeyService;
        this.accountEvent = accountEvent;
        this.logLedgerEvent = logLedgerEvent;
        this.accountGuaranteedBalanceTable = accountGuaranteedBalanceTable;
    }

    //TODO this lookup-method prevents the cyclic dependencies, need to be removed after refactoring the BlockchainProcessor class
    protected BlockchainProcessor lookupBlockchainProcessor() {
        if (this.blockchainProcessor == null) {
            this.blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        }
        return blockchainProcessor;
    }

    @Override
    public int getActiveLeaseCount() {
        return accountTable.getCount(new DbClause.NotNullClause("active_lessee_id"));
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
            PublicKey publicKey = accountPublicKeyService.loadPublicKey(dbKey, height);
            if (publicKey != null) {
                account = new Account(id, height);
                account.setPublicKey(publicKey);
            }
        }
        return account;
    }

    @Override
    public Account reloadAccount(Account account) {
        return accountTable.get(account.getDbKey());
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
    public Account addGenesisAccount(long id) {
        return addAccount(id, true);
    }

    @Override
    public Account addOrGetAccount(long id) {
        return addAccount(id, false);
    }

    /**
     * Create a new account. This account is not saved into the database but the public key of that one is saved.
     * This account will be saved during further operation of the balance changing. (The set of 'add to balance' operation).
     * @param id account id
     * @param isGenesis true if this account is a genesis account
     * @return new account
     */
    private Account addAccount(long id, boolean isGenesis) {
        Preconditions.checkArgument( id != 0, "Invalid accountId 0");
        DbKey dbKey = AccountTable.newKey(id);
        Account account = accountTable.get(dbKey);
        if (account == null) {
            account = accountTable.newEntity(dbKey);
            PublicKey publicKey = accountPublicKeyService.getPublicKey(dbKey);
            if (publicKey == null) {
                if(isGenesis){
                    publicKey = accountPublicKeyService.insertGenesisPublicKey(dbKey);
                }else {
                    publicKey = accountPublicKeyService.insertNewPublicKey(dbKey);
                }
            }
            account.setPublicKey(publicKey);
        }
        return account;
    }

    @Override
    public void update(Account account) {
        account.setHeight(blockchain.getHeight());
        if (account.getBalanceATM() == 0
                && account.getUnconfirmedBalanceATM() == 0
                && account.getForgedBalanceATM() == 0
                && account.getActiveLesseeId() == 0
                && account.getControls().isEmpty()) {
            accountTable.delete(account, true, blockchain.getHeight());
        } else {
            accountTable.insert(account);
        }
    }


    /**
     * The effective balance of an account is used as the basis for an account's forging calculations. An account's
     * effective balance consists of all tokens that have been stationary in that account for 1440 blocks. In addition,
     * the Account Leasing feature allows an account's effective balance to be assigned to another account for a temporary
     * period. The account effective balance is calculated from the confirmed balance by reducing all balance additions
     * during the last 1440 blocks.
     */
    @Override
    public long getEffectiveBalanceAPL(Account account, int height, boolean lock) {
        if (height <= EFFECTIVE_BALANCE_CONFIRMATIONS) {
            Account genesisAccount = getAccount(account.getId(), 0);
            return genesisAccount == null ? 0 : genesisAccount.getBalanceATM() / Constants.ONE_APL;
        }
        if (account.getPublicKey() == null) {
            account.setPublicKey(accountPublicKeyService.getPublicKey(AccountTable.newKey(account.getId())));
        }
        if (account.getPublicKey() == null || account.getPublicKey().getPublicKey() == null || height - account.getPublicKey().getHeight() <= EFFECTIVE_BALANCE_CONFIRMATIONS) {
            if(log.isTraceEnabled()) {
                log.trace(" height '{}' - this.publicKey.getHeight() '{}' ('{}') <= EFFECTIVE_BALANCE_CONFIRMATIONS '{}'",
                        height,
                        account.getPublicKey()!=null?account.getPublicKey().getHeight():null,
                        height - (account.getPublicKey()!=null?account.getPublicKey().getHeight():0),
                        EFFECTIVE_BALANCE_CONFIRMATIONS);
            }
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

    /**
     * The guaranteed balance of an account consists of all tokens that have been stationary in an account for 1440 (numberOfConfirmations) blocks.
     * Unlike the effective balance, this balance cannot be assigned to any other account.
     */
    @Override
    public long getGuaranteedBalanceATM(Account account, final int numberOfConfirmations, final int currentHeight) {
        sync.readLock();
        try {
            int height = currentHeight - numberOfConfirmations;
            if (height + blockchainConfig.getGuaranteedBalanceConfirmations() < lookupBlockchainProcessor().getMinRollbackHeight()
                    || height > blockchain.getHeight()) {
                if(log.isDebugEnabled()) {
                    log.debug("GuaranteedBalance Restriction: if ({} < {} || {} > {}) throw ex.",
                        height + blockchainConfig.getGuaranteedBalanceConfirmations(), blockchainProcessor.getMinRollbackHeight(),
                        height, blockchain.getHeight());
                }
                throw new IllegalArgumentException("Height " + height +
                        " not available for guaranteed balance calculation, blockchain.Height="+blockchain.getHeight());
            }
            Long sum = accountGuaranteedBalanceTable.getSumOfAdditions(account.getId(), height, currentHeight);
            if (sum == null) {
                return account.getBalanceATM();
            }
            return Math.max(Math.subtractExact(account.getBalanceATM(), sum), 0);
        }
        finally {
            sync.readUnlock();
        }
    }

    @Override
    public long getLessorsGuaranteedBalanceATM(Account account, int height) {
        List<Account> lessors = getLessors(account, height);
        long total = 0L;
        Map<Long, Long> lessorsAdditions = accountGuaranteedBalanceTable.getLessorsAdditions(
                lessors.stream().map(Account::getId).collect(Collectors.toList()),
                height, blockchain.getHeight());
        for (Account lessor : lessors) {
            long balance = lessor.getBalanceATM();
            Long additions = lessorsAdditions.get(lessor.getId());
            if (additions != null) {
                total += Math.max(balance - additions, 0);
            }else{
                total += balance;
            }
        }
        return total;
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
        return toList(getLessorsIterator(account));
    }

    @Override
    public List<Account> getLessors(Account account, int height) {
        return toList(getLessorsIterator(account, height));
    }

    private void logEntryConfirmed(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM) {
        LedgerEntry entry;
        if (feeATM != 0) {
            entry = new LedgerEntry(LedgerEvent.TRANSACTION_FEE, eventId, account.getId(),
                        LedgerHolding.APL_BALANCE, null, feeATM, account.getBalanceATM() - amountATM, blockchain.getLastBlock());
            logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY)).fire(entry);
        }
        if (amountATM != 0) {
            entry = new LedgerEntry(event, eventId, account.getId(),
                        LedgerHolding.APL_BALANCE, null, amountATM, account.getBalanceATM(), blockchain.getLastBlock());
            logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY)).fire(entry);
        }
    }

    private void logEntryUnconfirmed(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM) {
        LedgerEntry entry;
        if (feeATM != 0) {
            entry = new LedgerEntry(LedgerEvent.TRANSACTION_FEE, eventId, account.getId(),
                        LedgerHolding.UNCONFIRMED_APL_BALANCE, null, feeATM, account.getUnconfirmedBalanceATM() - amountATM, blockchain.getLastBlock());
            logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY)).fire(entry);
        }
        if (amountATM != 0) {
            entry = new LedgerEntry(event, eventId, account.getId(),
                        LedgerHolding.UNCONFIRMED_APL_BALANCE, null, amountATM, account.getUnconfirmedBalanceATM(), blockchain.getLastBlock());
            logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY)).fire(entry);
        }
    }

    @Override
    public void addToForgedBalanceATM(Account account, long amountATM){
        if (account.addToForgedBalanceATM(amountATM)){
            update(account);
        }
    }

    @Override
    public void addToBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (amountATM == 0 && feeATM == 0) {
            return;
        }
        if (feeATM != 0 && log.isTraceEnabled()){
            log.trace("Add c balance for {} from {} , amount - {}, total conf- {}, height- {}",
                    account.getId(), last3Stacktrace(),
                    amountATM, amountATM + account.getBalanceATM(), blockchain.getHeight());
        }
        long totalAmountATM = Math.addExact(amountATM, feeATM);
        account.setBalanceATM(Math.addExact(account.getBalanceATM(), totalAmountATM));
        accountGuaranteedBalanceTable.addToGuaranteedBalanceATM(account.getId(), totalAmountATM, blockchain.getHeight());
        AccountService.checkBalance(account.getId(), account.getBalanceATM(), account.getUnconfirmedBalanceATM());
        update(account);

        log.trace("Fire event {} account={}", AccountEventType.BALANCE, account);
        accountEvent.select(literal(AccountEventType.BALANCE)).fire(account);
        logEntryConfirmed(account, event, eventId, amountATM, feeATM);
    }

    @Override
    public  void addToBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM) {
        if(log.isTraceEnabled()) {
            log.trace("Add c balance for {} from {} , amount - {}, total conf- {}, height -{}", account.getId(), last3Stacktrace(), amountATM, amountATM + account.getBalanceATM(), blockchain.getHeight());
        }
        addToBalanceATM(account, event, eventId, amountATM, 0);
    }

    @Override
    public void addToBalanceAndUnconfirmedBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (amountATM == 0 && feeATM == 0) {
            return;
        }
        if (feeATM != 0 && log.isTraceEnabled()){
            log.trace("Add u balance for {} from {} , amount - {}, total unc {}, height - {}",
                    account.getId(), last3Stacktrace(),
                    amountATM, amountATM + account.getUnconfirmedBalanceATM(), blockchain.getHeight());
        }
        long totalAmountATM = Math.addExact(amountATM, feeATM);
        account.setBalanceATM(Math.addExact(account.getBalanceATM(), totalAmountATM));
        account.setUnconfirmedBalanceATM(Math.addExact(account.getUnconfirmedBalanceATM(), totalAmountATM));
        accountGuaranteedBalanceTable.addToGuaranteedBalanceATM(account.getId(), totalAmountATM, blockchain.getHeight());
        AccountService.checkBalance(account.getId(), account.getBalanceATM(), account.getUnconfirmedBalanceATM());
        update(account);

        log.trace("Fire event {} account={}", AccountEventType.BALANCE, account);
        accountEvent.select(literal(AccountEventType.BALANCE)).fire(account);
        log.trace("Fire event {} account={}", AccountEventType.UNCONFIRMED_BALANCE, account);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_BALANCE)).fire(account);

        if (event == null) {
            return;
        }
        logEntryUnconfirmed(account, event, eventId, amountATM, feeATM);
        logEntryConfirmed(account, event, eventId, amountATM, feeATM);
    }

    @Override
    public void addToBalanceAndUnconfirmedBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM) {
        if (log.isTraceEnabled()){
            log.trace("Add c and  u balance for {} from {} , amount - {}, total conf- {}, total unc {}, height {}",
                    account.getId(), last3Stacktrace(),
                    amountATM, amountATM + account.getBalanceATM(), amountATM + account.getUnconfirmedBalanceATM(), blockchain.getHeight());
        }
        addToBalanceAndUnconfirmedBalanceATM(account, event, eventId, amountATM, 0);
    }

    @Override
    public void addToUnconfirmedBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (amountATM == 0 && feeATM == 0) {
            return;
        }
        if (feeATM!=0 && log.isTraceEnabled()){
            log.trace("Add u balance for {} from {} , amount - {}, total unc {}, height - {}",
                    account.getId(), last3Stacktrace(),
                    amountATM, amountATM + account.getUnconfirmedBalanceATM(), blockchain.getHeight());
        }
        long totalAmountATM = Math.addExact(amountATM, feeATM);
        account.setUnconfirmedBalanceATM(Math.addExact(account.getUnconfirmedBalanceATM(), totalAmountATM));
        AccountService.checkBalance(account.getId(), account.getBalanceATM(), account.getUnconfirmedBalanceATM());
        update(account);

        log.trace("Fire event {} account={}", AccountEventType.UNCONFIRMED_BALANCE, account);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_BALANCE)).fire(account);

        if (event == null) {
            return;
        }
        logEntryUnconfirmed(account, event, eventId, amountATM, feeATM);
    }

    @Override
    public void addToUnconfirmedBalanceATM(Account account, LedgerEvent event, long eventId, long amountATM) {
        if (log.isTraceEnabled()){
            log.trace("Add u balance for {} from {} , amount - {}, total unc {}, height - {}",
                    account.getId(), last3Stacktrace(),
                    amountATM, amountATM + account.getUnconfirmedBalanceATM(), blockchain.getHeight());
        }
        addToUnconfirmedBalanceATM(account, event, eventId, amountATM, 0);
    }

    @Override
    public long getTotalAmountOnTopAccounts(int numberOfTopAccounts) {
        return accountTable.getTotalAmountOnTopAccounts(numberOfTopAccounts);
    }

    @Override
    public long getTotalAmountOnTopAccounts() {
        return getTotalAmountOnTopAccounts(100);
    }


    @Override
    public long getTotalNumberOfAccounts() {
        return accountTable.getTotalNumberOfAccounts();
    }

    @Override
    public List<Account> getTopHolders(int numberOfTopAccounts) {
        return accountTable.getTopHolders(numberOfTopAccounts);
    }

    @Override
    public long getTotalSupply() {
        return accountTable.getTotalSupply();
    }

    @Override
    public int getBlockchainHeight() {
        return blockchain.getHeight();
    }

    //Delegated from AccountPublicKeyService
    @Override
    public boolean setOrVerifyPublicKey(long accountId, byte[] key) {
        return accountPublicKeyService.setOrVerifyPublicKey(accountId, key);
    }

    @Override
    public byte[] getPublicKeyByteArray(long id) {
        return accountPublicKeyService.getPublicKeyByteArray(id);
    }
}

