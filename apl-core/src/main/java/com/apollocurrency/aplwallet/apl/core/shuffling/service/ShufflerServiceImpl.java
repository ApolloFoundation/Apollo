/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.service;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.shuffling.ShufflingEvent;
import com.apollocurrency.aplwallet.apl.core.shuffling.ShufflingEventType;
import com.apollocurrency.aplwallet.apl.core.shuffling.exception.ControlledAccountException;
import com.apollocurrency.aplwallet.apl.core.shuffling.exception.DuplicateShufflerException;
import com.apollocurrency.aplwallet.apl.core.shuffling.exception.InvalidRecipientException;
import com.apollocurrency.aplwallet.apl.core.shuffling.exception.InvalidStageException;
import com.apollocurrency.aplwallet.apl.core.shuffling.exception.ShufflerException;
import com.apollocurrency.aplwallet.apl.core.shuffling.exception.ShufflerLimitException;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffler;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCancellationAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRegistration;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingVerificationAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShufflerServiceImpl implements ShufflerService {
    private static final Logger LOG = getLogger(Shuffler.class);

    private final Map<String, Map<Long, Shuffler>> shufflingsMap = new HashMap<>();
    private final Map<Integer, Set<String>> expirations = new HashMap<>();

    private FeeCalculator feeCalculator = new FeeCalculator();

    private int maxShufflers;
    private PropertiesHolder propertiesHolder;
    private TransactionProcessor transactionProcessor;
    private Blockchain blockchain;
    private GlobalSync globalSync;
    private BlockchainProcessor blockchainProcessor;
    private ShufflingService shufflingService;
    private ShufflingParticipantService shufflingParticipantService;

    @Inject
    public ShufflerServiceImpl(PropertiesHolder propertiesHolder, TransactionProcessor transactionProcessor, Blockchain blockchain, GlobalSync globalSync, BlockchainProcessor blockchainProcessor, ShufflingService shufflingService, ShufflingParticipantService shufflingParticipantService) {
        this.propertiesHolder = propertiesHolder;
        this.maxShufflers = propertiesHolder.getIntProperty("apl.maxNumberOfShufflers");
        this.transactionProcessor = transactionProcessor;
        this.blockchain = blockchain;
        this.globalSync = globalSync;
        this.blockchainProcessor = blockchainProcessor;
        this.shufflingService = shufflingService;
        this.shufflingParticipantService = shufflingParticipantService;
    }

    @Override
    public Shuffler addOrGetShuffler(byte[] secretBytes, byte[] recipientPublicKey, byte[] shufflingFullHash) throws ShufflerException {
        String hash = Convert.toHexString(shufflingFullHash);
        long accountId = Account.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretBytes)));
        globalSync.writeLock();
        try {
            Map<Long, Shuffler> map = shufflingsMap.get(hash);
            if (map == null) {
                map = new HashMap<>();
                shufflingsMap.put(hash, map);
            }
            Shuffler shuffler = map.get(accountId);
            if (recipientPublicKey == null) {
                return shuffler;
            }
            if (shufflingsMap.size() > maxShufflers) {
                throw new ShufflerLimitException("Cannot run more than " + maxShufflers + " shufflers on the same node");
            }
            if (shuffler == null) {
                Shuffling shuffling = shufflingService.getShuffling(shufflingFullHash);
                if (shuffling == null && Account.getAccount(recipientPublicKey) != null) {
                    throw new InvalidRecipientException("Existing account cannot be used as shuffling recipient");
                }
                if (getRecipientShuffler(Account.getId(recipientPublicKey)) != null) {
                    throw new InvalidRecipientException("Another shuffler with the same recipient account already running");
                }
                if (map.size() >= (shuffling == null ? Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS : shuffling.getParticipantCount())) {
                    throw new ShufflerLimitException("Cannot run shufflers for more than " + map.size() + " accounts for this shuffling");
                }
                Account account = Account.getAccount(accountId);
                if (account != null && account.getControls().contains(Account.ControlType.PHASING_ONLY)) {
                    throw new ControlledAccountException("Cannot run a shuffler for an account under phasing only control");
                }
                shuffler = new Shuffler(secretBytes, recipientPublicKey, shufflingFullHash);
                if (shuffling != null) {
                    init(shuffler, shuffling);
                    clearExpiration(shuffling);
                }
                map.put(accountId, shuffler);
                LOG.info("Started shuffler for account {}, shuffling {}",
                        Long.toUnsignedString(accountId), Long.toUnsignedString(Convert.fullHashToId(shufflingFullHash)));
            } else if (!Arrays.equals(shuffler.getRecipientPublicKey(), recipientPublicKey)) {
                throw new DuplicateShufflerException("A shuffler with different recipientPublicKey already started");
            } else if (!Arrays.equals(shuffler.getShufflingFullHash(), shufflingFullHash)) {
                throw new DuplicateShufflerException("A shuffler with different shufflingFullHash already started");
            } else {
                LOG.info("Shuffler already started");
            }
            return shuffler;
        }
        finally {
            globalSync.writeUnlock();
        }
    }

    @Override
    public List<Shuffler> getAllShufflers() {
        List<Shuffler> shufflers = new ArrayList<>();
        globalSync.readLock();
        try {
            shufflingsMap.values().forEach(shufflerMap -> shufflers.addAll(shufflerMap.values()));
        }
        finally {
            globalSync.readUnlock();
        }
        return shufflers;
    }

    @Override
    public List<Shuffler> getShufflingShufflers(byte[] shufflingFullHash) {
        List<Shuffler> shufflers = new ArrayList<>();
        globalSync.readLock();
        try {
            Map<Long, Shuffler> shufflerMap = shufflingsMap.get(Convert.toHexString(shufflingFullHash));
            if (shufflerMap != null) {
                shufflers.addAll(shufflerMap.values());
            }
        }
        finally {
            globalSync.readUnlock();
        }
        return shufflers;
    }

    @Override
    public List<Shuffler> getAccountShufflers(long accountId) {
        List<Shuffler> shufflers = new ArrayList<>();
        globalSync.readLock();
        try {
            shufflingsMap.values().forEach(shufflerMap -> {
                Shuffler shuffler = shufflerMap.get(accountId);
                if (shuffler != null) {
                    shufflers.add(shuffler);
                }
            });
        }
        finally {
            globalSync.readUnlock();
        }
        return shufflers;
    }

    @Override
    public Shuffler getShuffler(long accountId, byte[] shufflingFullHash) {
        globalSync.readLock();
        try {
            Map<Long, Shuffler> shufflerMap = shufflingsMap.get(Convert.toHexString(shufflingFullHash));
            if (shufflerMap != null) {
                return shufflerMap.get(accountId);
            }
        }
        finally {
            globalSync.readUnlock();
        }
        return null;
    }

    @Override
    public Shuffler stopShuffler(long accountId, byte[] shufflingFullHash) {
        globalSync.writeLock();
        try {
            Map<Long, Shuffler> shufflerMap = shufflingsMap.get(Convert.toHexString(shufflingFullHash));
            if (shufflerMap != null) {
                return shufflerMap.remove(accountId);
            }
        }
        finally {
            globalSync.writeUnlock();
        }
        return null;
    }

    @Override
    public void stopAllShufflers() {
        globalSync.writeLock();
        try {
            shufflingsMap.clear();
        }
        finally {
            globalSync.writeUnlock();
        }
    }

    private Shuffler getRecipientShuffler(long recipientId) {
        globalSync.readLock();
        try {
            for (Map<Long, Shuffler> shufflerMap : shufflingsMap.values()) {
                for (Shuffler shuffler : shufflerMap.values()) {
                    if (Account.getId(shuffler.getRecipientPublicKey()) == recipientId) {
                        return shuffler;
                    }
                }
            }
            return null;
        }
        finally {
            globalSync.readUnlock();
        }
    }

    public void onShufflingCreated(@Observes @ShufflingEvent(ShufflingEventType.SHUFFLING_CREATED) Shuffling shuffling) {
        Map<Long, Shuffler> shufflerMap = getShufflers(shuffling);
        if (shufflerMap != null) {
            shufflerMap.values().forEach(shuffler -> {
                if (shuffler.getAccountId() != shuffling.getIssuerId()) {
                    try {
                        submitRegister(shuffler, shuffling);
                    }
                    catch (RuntimeException e) {
                        LOG.error(e.toString(), e);
                    }
                }
            });
            clearExpiration(shuffling);
        }
    }

    public void onShufflingProcessingAssigned(@Observes @ShufflingEvent(ShufflingEventType.SHUFFLING_PROCESSING_ASSIGNED) Shuffling shuffling) {
        Map<Long, Shuffler> shufflerMap = getShufflers(shuffling);
        if (shufflerMap != null) {
            Shuffler shuffler = shufflerMap.get(shuffling.getAssigneeAccountId());
            if (shuffler != null) {
                try {
                    submitProcess(shuffler, shuffling);
                }
                catch (RuntimeException e) {
                    LOG.error(e.toString(), e);
                }
            }
            clearExpiration(shuffling);
        }
    }

    public void onShufflingProcessingFinished(@Observes @ShufflingEvent(ShufflingEventType.SHUFFLING_PROCESSING_FINISHED) Shuffling shuffling) {
        Map<Long, Shuffler> shufflerMap = getShufflers(shuffling);
        if (shufflerMap != null) {
            shufflerMap.values().forEach(shuffler -> {
                try {
                    verify(shuffler, shuffling);
                }
                catch (RuntimeException e) {
                    LOG.error(e.toString(), e);
                }
            });
            clearExpiration(shuffling);
        }
    }

    public void onShufflingBlameStarted(@Observes @ShufflingEvent(ShufflingEventType.SHUFFLING_BLAME_STARTED) Shuffling shuffling) {
        Map<Long, Shuffler> shufflerMap = getShufflers(shuffling);
        if (shufflerMap != null) {
            shufflerMap.values().forEach(shuffler -> {
                try {
                    cancel(shuffler, shuffling);
                }
                catch (RuntimeException e) {
                    LOG.error(e.toString(), e);
                }
            });
            clearExpiration(shuffling);
        }
    }
    public void onShufflingDone(@Observes @ShufflingEvent(ShufflingEventType.SHUFFLING_DONE) Shuffling shuffling) {
        scheduleExpiration(shuffling);
    }

    public void onShufflingCancelled(@Observes @ShufflingEvent(ShufflingEventType.SHUFFLING_CANCELLED) Shuffling shuffling) {
        scheduleExpiration(shuffling);
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        Set<String> expired = expirations.get(block.getHeight());
        if (expired != null) {
            expired.forEach(shufflingsMap::remove);
            expirations.remove(block.getHeight());
        }
    }

    public void onBlockAccepted(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_ACCEPT) Block block) {

        shufflingsMap.values().forEach(shufflerMap -> shufflerMap.values().forEach(shuffler -> {
            if (shuffler.getFailedTransaction() != null) {
                try {
                    transactionProcessor.broadcast(shuffler.getFailedTransaction());
                    shuffler.setFailedTransaction(null);
                    shuffler.setFailureCause(null);
                }
                catch (AplException.ValidationException ignore) {
                }
            }
        }));
    }

    public void onRescanBegan(@Observes @BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
        stopAllShufflers();
    }

    private Map<Long, Shuffler> getShufflers(Shuffling shuffling) {
        return shufflingsMap.get(Convert.toHexString(shufflingService.getFullHash(shuffling)));
    }

    private void scheduleExpiration(Shuffling shuffling) {
        int expirationHeight = blockchain.getHeight() + 720;
        Set<String> shufflingIds = expirations.get(expirationHeight);
        if (shufflingIds == null) {
            shufflingIds = new HashSet<>();
            expirations.put(expirationHeight, shufflingIds);
        }
        shufflingIds.add(Convert.toHexString(shufflingService.getFullHash(shuffling)));
    }

    private void clearExpiration(Shuffling shuffling) {
        for (Set shufflingIds : expirations.values()) {
            if (shufflingIds.remove(shuffling.getId())) {
                return;
            }
        }
    }



    private void init(Shuffler shuffler, Shuffling shuffling) throws ShufflerException {
        ShufflingParticipant shufflingParticipant = shufflingParticipantService.getParticipant(shuffling.getId(), shuffler.getAccountId());
        switch (shuffling.getStage()) {
            case REGISTRATION:
                if (Account.getAccount(shuffler.getRecipientPublicKey()) != null) {
                    throw new InvalidRecipientException("Existing account cannot be used as shuffling recipient");
                }
                if (shufflingParticipant == null) {
                    submitRegister(shuffler, shuffling);
                }
                break;
            case PROCESSING:
                if (shufflingParticipant == null) {
                    throw new InvalidStageException("Account has not registered for this shuffling");
                }
                if (Account.getAccount(shuffler.getRecipientPublicKey()) != null) {
                    throw new InvalidRecipientException("Existing account cannot be used as shuffling recipient");
                }
                if (shuffler.getAccountId() == shuffling.getAssigneeAccountId()) {
                    submitProcess(shuffler, shuffling);
                }
                break;
            case VERIFICATION:
                if (shufflingParticipant == null) {
                    throw new InvalidStageException("Account has not registered for this shuffling");
                }
                if (shufflingParticipant.getState() == ShufflingParticipantService.State.PROCESSED) {
                    verify(shuffler, shuffling);
                }
                break;
            case BLAME:
                if (shufflingParticipant == null) {
                    throw new InvalidStageException("Account has not registered for this shuffling");
                }
                if (shufflingParticipant.getState() != ShufflingParticipantService.State.CANCELLED) {
                    cancel(shuffler, shuffling);
                }
                break;
            case DONE:
            case CANCELLED:
                scheduleExpiration(shuffling);
                break;
            default:
                throw new RuntimeException("Unsupported shuffling stage " + shuffling.getStage());
        }
        AplException.NotCurrentlyValidException failureCause = shuffler.getFailureCause();
        if (failureCause != null) {
            throw new ShufflerException(failureCause.getMessage(), failureCause);
        }
    }

    private void verify(Shuffler shuffler, Shuffling shuffling) {
        ShufflingParticipant shufflingParticipant = shufflingParticipantService.getParticipant(shuffling.getId(), shuffler.getAccountId());
        if (shufflingParticipant != null && shufflingParticipant.getIndex() != shuffling.getParticipantCount() - 1) {
            boolean found = false;
            for (byte[] key : shuffling.getRecipientPublicKeys()) {
                if (Arrays.equals(key, shuffler.getRecipientPublicKey())) {
                    found = true;
                    break;
                }
            }
            if (found) {
                submitVerify(shuffler, shuffling);
            } else {
                submitCancel(shuffler, shuffling);
            }
        }
    }

    private void cancel(Shuffler shuffler, Shuffling shuffling) {
        if (shuffler.getAccountId() == shuffling.getAssigneeAccountId()) {
            return;
        }
        ShufflingParticipant shufflingParticipant = shufflingParticipantService.getParticipant(shuffling.getId(), shuffler.getAccountId());
        if (shufflingParticipant == null || shufflingParticipant.getIndex() == shuffling.getParticipantCount() - 1) {
            return;
        }
        if (shufflingParticipantService.getData(shuffling.getId(), shuffler.getAccountId()) == null) {
            return;
        }
        submitCancel(shuffler, shuffling);
    }

    private void submitRegister(Shuffler shuffler, Shuffling shuffling) {
        LOG.debug("Account {} registering for shuffling {}", Long.toUnsignedString(shuffler.getAccountId()), Long.toUnsignedString(shuffling.getId()));
        ShufflingRegistration attachment = new ShufflingRegistration(shuffler.getShufflingFullHash());
        submitTransaction(shuffler, attachment);
    }

    private void submitProcess(Shuffler shuffler, Shuffling shuffling) {
        LOG.debug("Account {} processing shuffling {}", Long.toUnsignedString(shuffler.getAccountId()), Long.toUnsignedString(shuffling.getId()));
        ShufflingAttachment attachment = shufflingService.process(shuffling, shuffler.getAccountId(), shuffler.getSecretBytes(), shuffler.getRecipientPublicKey());
        submitTransaction(shuffler, attachment);
    }

    private void submitVerify(Shuffler shuffler, Shuffling shuffling) {
        LOG.debug("Account {} verifying shuffling {}", Long.toUnsignedString(shuffler.getAccountId()), Long.toUnsignedString(shuffling.getId()));
        ShufflingVerificationAttachment attachment = new ShufflingVerificationAttachment(shuffling.getId(), shufflingService.getStateHash(shuffling));
        submitTransaction(shuffler, attachment);
    }

    private void submitCancel(Shuffler shuffler, Shuffling shuffling) {
        LOG.debug("Account {} cancelling shuffling {}", Long.toUnsignedString(shuffler.getAccountId()), Long.toUnsignedString(shuffling.getId()));
        ShufflingCancellationAttachment attachment = shufflingService.revealKeySeeds(shuffling, shuffler.getSecretBytes(), shuffling.getAssigneeAccountId(),
                shufflingService.getStateHash(shuffling));
        submitTransaction(shuffler, attachment);
    }

    private void submitTransaction(Shuffler shuffler, ShufflingAttachment attachment) {
        if (blockchainProcessor.isProcessingBlock()) {
            if (hasUnconfirmedTransaction(shuffler, attachment, transactionProcessor.getWaitingTransactions())) {
                LOG.debug("Transaction already submitted");
                return;
            }
        } else {
            try (DbIterator<UnconfirmedTransaction> unconfirmedTransactions = transactionProcessor.getAllUnconfirmedTransactions()) {
                if (hasUnconfirmedTransaction(shuffler, attachment, unconfirmedTransactions)) {
                    LOG.debug("Transaction already submitted");
                    return;
                }
            }
        }
        try {
            Transaction.Builder builder = Transaction.newTransactionBuilder(Crypto.getPublicKey(Crypto.getKeySeed(shuffler.getSecretBytes())), 0, 0,
                    (short) 1440, attachment, blockchain.getLastBlockTimestamp());

            Transaction transaction = builder.build(null);
            transaction.setFeeATM(feeCalculator.getMinimumFeeATM(transaction, blockchain.getHeight()));
            transaction.sign(Crypto.getKeySeed(shuffler.getSecretBytes()));
            shuffler.setFailedTransaction(null);
            shuffler.setFailureCause(null);
            Account participantAccount = Account.getAccount(shuffler.getAccountId());
            if (participantAccount == null || transaction.getFeeATM() > participantAccount.getUnconfirmedBalanceATM()) {
                shuffler.setFailedTransaction(transaction);
                shuffler.setFailureCause(new AplException.NotCurrentlyValidException("Insufficient balance"));
                LOG.debug("Error submitting shuffler transaction", shuffler.getFailureCause());
            }
            try {
                transactionProcessor.broadcast(transaction);
            }
            catch (AplException.NotCurrentlyValidException e) {
                shuffler.setFailedTransaction(transaction);
                shuffler.setFailureCause(e);
                LOG.debug("Error submitting shuffler transaction", e);
            }
        }
        catch (AplException.ValidationException e) {
            LOG.error("Fatal error submitting shuffler transaction", e);
        }
    }

    private boolean hasUnconfirmedTransaction(Shuffler shuffler, ShufflingAttachment shufflingAttachment, Iterable<UnconfirmedTransaction> unconfirmedTransactions) {
        for (UnconfirmedTransaction unconfirmedTransaction : unconfirmedTransactions) {
            if (unconfirmedTransaction.getSenderId() != shuffler.getAccountId()) {
                continue;
            }
            Attachment attachment = unconfirmedTransaction.getAttachment();
            if (!attachment.getClass().equals(shufflingAttachment.getClass())) {
                continue;
            }
            if (Arrays.equals(shufflingAttachment.getShufflingStateHash(), ((ShufflingAttachment) attachment).getShufflingStateHash())) {
                return true;
            }
        }
        return false;
    }
}
