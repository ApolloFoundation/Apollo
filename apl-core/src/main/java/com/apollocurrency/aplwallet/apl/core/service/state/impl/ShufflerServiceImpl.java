/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShufflingEvent;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionSigner;
import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffler;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipantState;
import com.apollocurrency.aplwallet.apl.core.exception.ShufflerException;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflerService;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCancellationAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRegistration;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingVerificationAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionVersionValidator.DEFAULT_VERSION;

/**
 * The Shuffler service
 * <p>
 * NOTICE: The current service supports the transaction V1 signing, it doesn't support multi-sig.
 * See the document signer instantiating routine in the constructor.
 */
@Slf4j
@Singleton
public class ShufflerServiceImpl implements ShufflerService {

    private final MemPool memPool;
    private final Blockchain blockchain;
    private final GlobalSync globalSync;
    private final ShufflingService shufflingService;
    private final FeeCalculator feeCalculator;
    private final BlockchainProcessor blockchainProcessor;
    private final AccountService accountService;
    private final TransactionBuilderFactory transactionBuilderFactory;
    private final int MAX_SHUFFLERS;
    //TODO Use TransactionVersionValidator#getActualVersion()
    private final int transactionVersion = DEFAULT_VERSION;//transaction version during the shuffling routine

    private final TransactionSigner signerService;

    private final Map<String, Map<Long, Shuffler>> shufflingsMap = new ConcurrentHashMap<>();
    private final Map<Integer, Set<String>> expirations = new ConcurrentHashMap<>();

    @Inject
    public ShufflerServiceImpl(PropertiesHolder propertiesLoade, MemPool memPool,
                               Blockchain blockchain, GlobalSync globalSync, ShufflingService shufflingService,
                               FeeCalculator feeCalculator, BlockchainProcessor blockchainProcessor,
                               AccountService accountService, TransactionBuilderFactory transactionBuilderFactory,
                               TransactionSigner signerService) {
        this.memPool = memPool;
        this.blockchain = blockchain;
        this.globalSync = globalSync;
        this.shufflingService = shufflingService;
        this.feeCalculator = feeCalculator;
        this.blockchainProcessor = blockchainProcessor;
        this.accountService = accountService;
        this.MAX_SHUFFLERS = propertiesLoade.getIntProperty("apl.maxNumberOfShufflers");
        this.transactionBuilderFactory = transactionBuilderFactory;
        this.signerService = signerService;
    }

    @Override
    public Shuffler addOrGetShuffler(byte[] secretBytes, byte[] recipientPublicKey, byte[] shufflingFullHash) throws ShufflerException {
        String hash = Convert.toHexString(shufflingFullHash);
        long accountId = AccountService.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretBytes)));
        globalSync.writeLock();
        try {
            Map<Long, Shuffler> map = shufflingsMap.get(hash);
            if (map == null) {
                map = new HashMap<>();
                shufflingsMap.put(hash, map);
                init();
            }
            Shuffler shuffler = map.get(accountId);
            if (recipientPublicKey == null) {
                return shuffler;
            }
            if (shufflingsMap.size() > MAX_SHUFFLERS) {
                throw new ShufflerException.ShufflerLimitException("Cannot run more than " + MAX_SHUFFLERS + " shufflers on the same node");
            }
            if (shuffler == null) {
                Shuffling shuffling = shufflingService.getShuffling(shufflingFullHash);
                if (shuffling == null && accountService.getAccount(recipientPublicKey) != null) {
                    throw new ShufflerException.InvalidRecipientException("Existing account cannot be used as shuffling recipient");
                }
                if (getRecipientShuffler(AccountService.getId(recipientPublicKey)) != null) {
                    throw new ShufflerException.InvalidRecipientException("Another shuffler with the same recipient account already running");
                }
                if (map.size() >= (shuffling == null ? Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS : shuffling.getParticipantCount())) {
                    throw new ShufflerException.ShufflerLimitException("Cannot run shufflers for more than " + map.size() + " accounts for this shuffling");
                }
                Account account = accountService.getAccount(accountId);
                if (account != null && account.getControls().contains(AccountControlType.PHASING_ONLY)) {
                    throw new ShufflerException.ControlledAccountException("Cannot run a shuffler for an account under phasing only control");
                }
                shuffler = new Shuffler(secretBytes, recipientPublicKey, shufflingFullHash);
                if (shuffling != null) {
                    init(shuffling, shuffler);
                    clearExpiration(shuffling);
                }
                map.put(accountId, shuffler);
                log.info("Started shuffler for account {}, shuffling {}",
                    Long.toUnsignedString(accountId), Long.toUnsignedString(Convert.transactionFullHashToId(shufflingFullHash)));
            } else if (!Arrays.equals(shuffler.getRecipientPublicKey(), recipientPublicKey)) {
                throw new ShufflerException.DuplicateShufflerException("A shuffler with different recipientPublicKey already started");
            } else if (!Arrays.equals(shuffler.getShufflingFullHash(), shufflingFullHash)) {
                throw new ShufflerException.DuplicateShufflerException("A shuffler with different shufflingFullHash already started");
            } else {
                log.info("Shuffler already started by accountId = {}", accountId);
            }
            log.trace("Shuffler created by accountId = {}", accountId);
            return shuffler;
        } finally {
            globalSync.writeUnlock();
        }
    }

    @Override
    public List<Shuffler> getAllShufflers() {
        List<Shuffler> shufflers = new ArrayList<>();
        globalSync.readLock();
        try {
            shufflingsMap.values().forEach(shufflerMap -> shufflers.addAll(shufflerMap.values()));
        } finally {
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
        } finally {
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
        } finally {
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
        } finally {
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
                log.trace("Shuffler is stopped by accountId = {}", accountId);
                return shufflerMap.remove(accountId);
            }
        } finally {
            globalSync.writeUnlock();
        }
        return null;
    }

    @Override
    public void stopAllShufflers() {
        globalSync.writeLock();
        try {
            log.trace("Stopped all [{}] shufflers", shufflingsMap.size());
            shufflingsMap.clear();
        } finally {
            globalSync.writeUnlock();
        }
    }

    @Override
    public Map<String, Map<Long, Shuffler>> getShufflingsMap() {
        return new HashMap<>(shufflingsMap);
    }

    @Override
    public void removeShufflingsByHash(String hash) {
        shufflingsMap.remove(hash);
    }

    @Override
    public Map<Integer, Set<String>> getExpirations() {
        return new HashMap<>(expirations);
    }

    @Override
    public void removeExpirationsByHeight(Integer height) {
        expirations.remove(height);
    }

    private void init() {
        shufflingService.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = getShufflers(shuffling);
            if (shufflerMap != null) {
                shufflerMap.values().forEach(shuffler -> {
                    if (shuffler.getAccountId() != shuffling.getIssuerId()) {
                        try {
                            submitRegister(shuffler, shuffling);
                        } catch (RuntimeException e) {
                            log.error(e.toString(), e);
                        }
                    }
                });
                clearExpiration(shuffling);
            }
        }, ShufflingEvent.SHUFFLING_CREATED);

        shufflingService.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = getShufflers(shuffling);
            if (shufflerMap != null) {
                Shuffler shuffler = shufflerMap.get(shuffling.getAssigneeAccountId());
                if (shuffler != null) {
                    try {
                        submitProcess(shuffler, shuffling);
                    } catch (RuntimeException e) {
                        log.error(e.toString(), e);
                    }
                }
                clearExpiration(shuffling);
            }
        }, ShufflingEvent.SHUFFLING_PROCESSING_ASSIGNED);

        shufflingService.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = getShufflers(shuffling);
            if (shufflerMap != null) {
                shufflerMap.values().forEach(shuffler -> {
                    try {
                        verify(shuffler, shuffling);
                    } catch (RuntimeException e) {
                        log.error(e.toString(), e);
                    }
                });
                clearExpiration(shuffling);
            }
        }, ShufflingEvent.SHUFFLING_PROCESSING_FINISHED);

        shufflingService.addListener(shuffling -> {
            Map<Long, Shuffler> shufflerMap = getShufflers(shuffling);
            if (shufflerMap != null) {
                shufflerMap.values().forEach(shuffler -> {
                    try {
                        cancel(shuffler, shuffling);
                    } catch (RuntimeException e) {
                        log.error(e.toString(), e);
                    }
                });
                clearExpiration(shuffling);
            }
        }, ShufflingEvent.SHUFFLING_BLAME_STARTED);

        shufflingService.addListener(this::scheduleExpiration, ShufflingEvent.SHUFFLING_DONE);

        shufflingService.addListener(this::scheduleExpiration, ShufflingEvent.SHUFFLING_CANCELLED);
    }

    private void init(Shuffling shuffling, Shuffler shuffler) throws ShufflerException {
        ShufflingParticipant shufflingParticipant = shufflingService.getParticipant(shuffling.getId(), shuffler.getAccountId());
        switch (shuffling.getStage()) {
            case REGISTRATION:
                if (accountService.getAccount(shuffler.getRecipientPublicKey()) != null) {
                    throw new ShufflerException.InvalidRecipientException("Existing account cannot be used as shuffling recipient");
                }
                if (shufflingParticipant == null) {
                    submitRegister(shuffler, shuffling);
                }
                break;
            case PROCESSING:
                if (shufflingParticipant == null) {
                    throw new ShufflerException.InvalidStageException("Account has not registered for this shuffling");
                }
                if (accountService.getAccount(shuffler.getRecipientPublicKey()) != null) {
                    throw new ShufflerException.InvalidRecipientException("Existing account cannot be used as shuffling recipient");
                }
                if (shuffler.getAccountId() == shuffling.getAssigneeAccountId()) {
                    submitProcess(shuffler, shuffling);
                }
                break;
            case VERIFICATION:
                if (shufflingParticipant == null) {
                    throw new ShufflerException.InvalidStageException("Account has not registered for this shuffling");
                }
                if (shufflingParticipant.getState() == ShufflingParticipantState.PROCESSED) {
                    verify(shuffler, shuffling);
                }
                break;
            case BLAME:
                if (shufflingParticipant == null) {
                    throw new ShufflerException.InvalidStageException("Account has not registered for this shuffling");
                }
                if (shufflingParticipant.getState() != ShufflingParticipantState.CANCELLED) {
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
        if (shuffler.getFailureCause() != null) {
            throw new ShufflerException(shuffler.getFailureCause().getMessage(), shuffler.getFailureCause());
        }
    }


    private Shuffler getRecipientShuffler(long recipientId) {
        globalSync.readLock();
        try {
            for (Map<Long, Shuffler> shufflerMap : shufflingsMap.values()) {
                for (Shuffler shuffler : shufflerMap.values()) {
                    if (AccountService.getId(shuffler.getRecipientPublicKey()) == recipientId) {
                        return shuffler;
                    }
                }
            }
            return null;
        } finally {
            globalSync.readUnlock();
        }
    }

    private Map<Long, Shuffler> getShufflers(Shuffling shuffling) {
        return shufflingsMap.get(Convert.toHexString(shufflingService.getFullHash(shuffling.getId())));
    }

    private void scheduleExpiration(Shuffling shuffling) {
        int expirationHeight = blockchain.getHeight() + 720;
        Set<String> shufflingIds = expirations.get(expirationHeight);
        if (shufflingIds == null) {
            shufflingIds = new HashSet<>();
            expirations.put(expirationHeight, shufflingIds);
        }
        shufflingIds.add(Convert.toHexString(shufflingService.getFullHash(shuffling.getId())));
    }

    private void clearExpiration(Shuffling shuffling) {
        for (Set shufflingIds : expirations.values()) {
            if (shufflingIds.remove(shuffling.getId())) {
                return;
            }
        }
    }

    private void verify(Shuffler shuffler, Shuffling shuffling) {
        ShufflingParticipant shufflingParticipant = shufflingService.getParticipant(shuffling.getId(), shuffler.getAccountId());
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
        ShufflingParticipant shufflingParticipant = shufflingService.getParticipant(shuffling.getId(), shuffler.getAccountId());
        if (shufflingParticipant == null || shufflingParticipant.getIndex() == shuffling.getParticipantCount() - 1) {
            return;
        }
        if (shufflingService.getData(shuffling.getId(), shuffler.getAccountId()) == null) {
            return;
        }
        submitCancel(shuffler, shuffling);
    }

    private void submitRegister(Shuffler shuffler, Shuffling shuffling) {
        log.debug("Account {} registering for shuffling {}", Long.toUnsignedString(shuffler.getAccountId()), Long.toUnsignedString(shuffling.getId()));
        ShufflingRegistration attachment = new ShufflingRegistration(shuffler.getShufflingFullHash());
        submitTransaction(shuffler, attachment);
    }

    private void submitProcess(Shuffler shuffler, Shuffling shuffling) {
        log.debug("Account {} processing shuffling {}", Long.toUnsignedString(shuffler.getAccountId()), Long.toUnsignedString(shuffling.getId()));
        ShufflingAttachment attachment = shufflingService.processShuffling(shuffling, shuffler.getAccountId(), shuffler.getSecretBytes(), shuffler.getRecipientPublicKey());
        submitTransaction(shuffler, attachment);
    }

    private void submitVerify(Shuffler shuffler, Shuffling shuffling) {
        log.debug("Account {} verifying shuffling {}", Long.toUnsignedString(shuffler.getAccountId()), Long.toUnsignedString(shuffling.getId()));
        ShufflingVerificationAttachment attachment = new ShufflingVerificationAttachment(shuffling.getId(), shufflingService.getStageHash(shuffling));
        submitTransaction(shuffler, attachment);
    }

    private void submitCancel(Shuffler shuffler, Shuffling shuffling) {
        log.debug("Account {} cancelling shuffling {}", Long.toUnsignedString(shuffler.getAccountId()), Long.toUnsignedString(shuffling.getId()));
        ShufflingCancellationAttachment attachment = shufflingService.revealKeySeeds(shuffling, shuffler.getSecretBytes(), shuffling.getAssigneeAccountId(),
            shufflingService.getStageHash(shuffling));
        submitTransaction(shuffler, attachment);
    }

    private void submitTransaction(Shuffler shuffler, ShufflingAttachment attachment) {
        if (blockchainProcessor.isProcessingBlock()) {
            if (hasUnconfirmedTransaction(shuffler, attachment)) {
                log.debug("Transaction already submitted");
                return;
            }
        } else {
            if (hasUnconfirmedTransaction(shuffler, attachment)) {
                log.debug("Transaction already submitted");
                return;
            }
        }

        try {
            int timestamp = blockchain.getLastBlockTimestamp();
            byte[] keySeed = Crypto.getKeySeed(shuffler.getSecretBytes());
            Transaction.Builder builder = transactionBuilderFactory.newUnsignedTransactionBuilder(transactionVersion,
                Crypto.getPublicKey(keySeed),
                0, 0,
                (short) 1440, attachment, timestamp)
                .ecBlockData(blockchain.getECBlock(timestamp));

            Transaction transaction = builder.build();
            transaction.setFeeATM(feeCalculator.getMinimumFeeATM(transaction, blockchain.getHeight()));

            signerService.sign(transaction, keySeed);

            shuffler.setFailedTransaction(null);
            shuffler.setFailureCause(null);
            Account participantAccount = accountService.getAccount(shuffler.getAccountId());
            if (participantAccount == null || transaction.getFeeATM() > participantAccount.getUnconfirmedBalanceATM()) {
                shuffler.setFailedTransaction(transaction);
                shuffler.setFailureCause(new AplException.NotCurrentlyValidException("Insufficient balance"));
                log.debug("Error submitting shuffler transaction", shuffler.getFailureCause());
            }
            try {
                boolean broadcasted = memPool.softBroadcast(transaction);
                if (!broadcasted) {
                    shuffler.setFailedTransaction(transaction);
                    log.info("Broadcast of shuffling transaction was not successful, will try again later");
                    return;
                }
                log.trace("Submitted Shuffling Tx: id: {}, participantAccount:{}, atm: {}, deadline: {}",
                    transaction.getId(), participantAccount, transaction.getAmountATM(), transaction.getDeadline());
            } catch (AplException.NotCurrentlyValidException e) {
                shuffler.setFailedTransaction(transaction);
                shuffler.setFailureCause(e);
                log.debug("Error submitting shuffler transaction", e);
            }
        } catch (AplException.ValidationException e) {
            log.error("Fatal error submitting shuffler transaction", e);
        }
    }

    private boolean hasUnconfirmedTransaction(Shuffler shuffler, ShufflingAttachment shufflingAttachment) {
        List<UnconfirmedTransaction> list = CollectionUtil.toList(memPool.getAllProcessedStream());
        for (UnconfirmedTransaction unconfirmedTransaction : list) {
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
