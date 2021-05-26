/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShufflingEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShufflingParticipantEvent;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingDataTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingParticipantTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingRepository;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingData;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipantState;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCancellationAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCreation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingProcessingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRecipientsAttachment;
import com.apollocurrency.aplwallet.apl.crypto.AnonymouslyEncryptedData;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Slf4j
@Singleton
public class ShufflingServiceImpl implements ShufflingService {

    private final ShufflingRepository shufflingRepository;
    private final ShufflingDataTable shufflingDataTable;
    private final ShufflingParticipantTable participantTable;
    private final BlockchainConfig blockchainConfig;
    private final TimeService timeService;
    private final Blockchain blockchain;
    private final GlobalSync globalSync;
    private final AccountService accountService;
    private final AccountPublicKeyService accountPublicKeyService;
    private final ShardDao shardDao;
    private final boolean deleteFinished;

    private static final Listeners<ShufflingParticipant, ShufflingParticipantEvent> participantListeners = new Listeners<>();
    private static final Listeners<Shuffling, ShufflingEvent> shufflingListeners = new Listeners<>();

    @Inject
    public ShufflingServiceImpl(
                                ShufflingRepository shufflingRepository,
                                ShufflingDataTable shufflingDataTable,
                                ShufflingParticipantTable participantTable,
                                BlockchainConfig blockchainConfig,
                                TimeService timeService,
                                Blockchain blockchain,
                                GlobalSync globalSync,
                                AccountService accountService,
                                AccountPublicKeyService accountPublicKeyService,
                                ShardDao shardDao,
                                PropertiesHolder propertiesHolder
                                ) {
        this.shufflingRepository = shufflingRepository;
        this.shufflingDataTable = shufflingDataTable;
        this.participantTable = participantTable;
        this.blockchainConfig = blockchainConfig;
        this.timeService = timeService;
        this.blockchain = blockchain;
        this.globalSync = globalSync;
        this.accountService = accountService;
        this.accountPublicKeyService = accountPublicKeyService;
        this.shardDao = shardDao;

        this.deleteFinished = propertiesHolder.getBooleanProperty("apl.deleteFinishedShufflings");
    }

    @Override
    public byte[][] getData(long shufflingId, long accountId) {
        return shufflingDataTable.getData(shufflingId, accountId);
    }

    @Override
    public void restoreData(long shufflingId, long accountId, byte[][] data, int timestamp, int height) {
        shufflingDataTable.restoreData(shufflingId, accountId, data, timestamp, height);
    }

    @Override
    public void setData(ShufflingParticipant participant, byte[][] data, int timestamp) {
        if (data != null && timeService.getEpochTime() - timestamp < blockchainConfig.getMaxPrunableLifetime() && getData(participant.getShufflingId(), participant.getAccountId()) == null) {
            shufflingDataTable.insert(new ShufflingData(participant.getShufflingId(), participant.getAccountId(), data, timestamp, blockchain.getHeight()));
        }
    }

    @Override
    public List<ShufflingParticipant> getParticipants(long shufflingId) {
        return participantTable.getParticipants(shufflingId);
    }

    @Override
    public ShufflingParticipant getParticipant(long shufflingId, long accountId) {
        return participantTable.getParticipant(shufflingId, accountId);
    }

    @Override
    public ShufflingParticipant getLastParticipant(long shufflingId) {
        return participantTable.getLastParticipant(shufflingId);
    }

    @Override
    public void addParticipant(long shufflingId, long accountId, int index) {
        ShufflingParticipant participant = new ShufflingParticipant(shufflingId, accountId, index, blockchain.getHeight());
        participantTable.addParticipant(participant);

        participantListeners.notify(participant, ShufflingParticipantEvent.PARTICIPANT_REGISTERED);
    }

    @Override
    public int getVerifiedCount(long shufflingId) {
        return participantTable.getVerifiedCount(shufflingId);
    }

    @Override
    public void changeStatusToProcessed(ShufflingParticipant participant, byte[] dataTransactionFullHash, byte[] dataHash) {
        if (participant.getDataTransactionFullHash() != null) {
            throw new IllegalStateException("dataTransactionFullHash already set");
        }
        participant.setState(ShufflingParticipantState.PROCESSED);
        participant.setDataTransactionFullHash(dataTransactionFullHash);
        if (dataHash != null) {
            if (participant.getDataHash() != null) {
                throw new IllegalStateException("dataHash already set");
            }
            participant.setDataHash(dataHash);
        }
        participant.setHeight(blockchain.getHeight());
        participantTable.insert(participant);

        participantListeners.notify(participant, ShufflingParticipantEvent.PARTICIPANT_PROCESSED);
    }

    @Override
    public void changeStatusToVerified(ShufflingParticipant participant) {
        participant.setState(ShufflingParticipantState.VERIFIED);
        participant.setHeight(blockchain.getHeight());
        participantTable.insert(participant);

        participantListeners.notify(participant, ShufflingParticipantEvent.PARTICIPANT_VERIFIED);
    }

    @Override
    public void changeStatusToCancel(ShufflingParticipant participant, byte[][] blameData, byte[][] keySeeds) {
        if (participant.getKeySeeds().length > 0) {
            throw new IllegalStateException("keySeeds already set");
        }
        participant.setBlameData(blameData);
        participant.setKeySeeds(keySeeds);
        participant.setState(ShufflingParticipantState.CANCELLED);
        participant.setHeight(blockchain.getHeight());
        participantTable.insert(participant);

        participantListeners.notify(participant, ShufflingParticipantEvent.PARTICIPANT_CANCELLED);
    }

    @Override
    public ShufflingParticipant getPreviousParticipant(ShufflingParticipant participant) {
        if (participant.getIndex() == 0) {
            return null;
        }

        return participantTable.getPreviousParticipant(participant);
    }

    @Override
    public boolean delete(ShufflingParticipant participant) {
        participant.setHeight(blockchain.getHeight());
        return participantTable.deleteAtHeight(participant, blockchain.getHeight());
    }

    @Override
    public void addShuffling(Transaction transaction, ShufflingCreation attachment) {
        Shuffling shuffling = new Shuffling(transaction, attachment, blockchain.getHeight());
        shufflingRepository.insert(shuffling);
        addParticipant(shuffling.getId(), transaction.getSenderId(), 0);
        shufflingListeners.notify(shuffling, ShufflingEvent.SHUFFLING_CREATED);
    }

    @Override
    public void save(Shuffling shuffling){
        updateHeightAndInsert(shuffling);
    }

    @Override
    public int getShufflingCount() {
        return shufflingRepository.getCount();
    }

    @Override
    public int getShufflingActiveCount() {
        return shufflingRepository.getActiveCount();
    }

    @Override
    public boolean addListener(Listener<Shuffling> listener, ShufflingEvent eventType) {
        return shufflingListeners.addListener(listener, eventType);
    }

    @Override
    public boolean removeListener(Listener<Shuffling> listener, ShufflingEvent eventType) {
        return shufflingListeners.removeListener(listener, eventType);
    }

    @Override
    public List<Shuffling> getAll(int from, int to) {
        return shufflingRepository.extractAll(from, to);
    }

    @Override
    public List<Shuffling> getActiveShufflings(int from, int to) {
        return shufflingRepository.getActiveShufflings(from, to);
    }

    @Override
    public List<Shuffling> getActiveShufflings() {
        return shufflingRepository.getActiveShufflings(0, -1);
    }

    @Override
    public List<Shuffling> getFinishedShufflings(int from, int to) {
        return shufflingRepository.getFinishedShufflings(from, to);
    }

    @Override
    public byte[] getFullHash(long shufflingId) {
        return blockchain.getFullHash(shufflingId);
    }

    @Override
    public Shuffling getShuffling(long shufflingId) {
        return shufflingRepository.get(shufflingId);
    }

    @Override
    public Shuffling getShuffling(byte[] fullHash) {
        long shufflingId = Convert.transactionFullHashToId(fullHash);
        Shuffling shuffling = shufflingRepository.get(shufflingId);
        if (shuffling != null && !Arrays.equals(getFullHash(shuffling.getId()), fullHash)) {
            log.debug("Shuffling with different hash {} but same id found for hash {}",
                Convert.toHexString(getFullHash(shuffling.getId())), Convert.toHexString(fullHash));
            return null;
        }
        return shuffling;
    }

    @Override
    public ShufflingAttachment processShuffling(Shuffling shuffling, final long accountId, final byte[] secretBytes, final byte[] recipientPublicKey) {
        byte[][] data = Convert.EMPTY_BYTES;
        byte[] shufflingStateHash = null;
        int participantIndex = 0;
        List<ShufflingParticipant> shufflingParticipants = new ArrayList<>();
        globalSync.readLock();
        // Read the participant list for the shuffling
        try {
        List<ShufflingParticipant> participants = getParticipants(shuffling.getId());
            for (ShufflingParticipant participant : participants) {
                shufflingParticipants.add(participant);
                if (participant.getNextAccountId() == accountId) {
                    data = getData(participant.getShufflingId(), participant.getAccountId());
                    shufflingStateHash = participant.getDataTransactionFullHash();
                    participantIndex = shufflingParticipants.size();
                }
            }
            if (shufflingStateHash == null) {
                shufflingStateHash = getParticipantsHash(shufflingParticipants);
            }
        } finally {
            globalSync.readUnlock();
        }
        boolean isLast = participantIndex == shuffling.getParticipantCount() - 1;
        // decrypt the tokens bundled in the current data
        List<byte[]> outputDataList = new ArrayList<>();
        for (byte[] bytes : data) {
            AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
            try {
                byte[] decrypted = encryptedData.decrypt(secretBytes);
                outputDataList.add(decrypted);
            } catch (Exception e) {
                log.info("Decryption failed", e);
                return isLast ? new ShufflingRecipientsAttachment(shuffling.getId(), Convert.EMPTY_BYTES, shufflingStateHash)
                    : new ShufflingProcessingAttachment(shuffling.getId(), Convert.EMPTY_BYTES, shufflingStateHash);
            }
        }
        // Calculate the token for the current sender by iteratively encrypting it using the public key of all the participants
        // which did not perform shuffle processing yet
        byte[] bytesToEncrypt = recipientPublicKey;
        byte[] nonce = Convert.toBytes(shuffling.getId());
        for (int i = shufflingParticipants.size() - 1; i > participantIndex; i--) {
            ShufflingParticipant participant = shufflingParticipants.get(i);
            byte[] participantPublicKey = accountService.getPublicKeyByteArray(participant.getAccountId());
            AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.encrypt(bytesToEncrypt, secretBytes, participantPublicKey, nonce);
            bytesToEncrypt = encryptedData.getBytes();
        }
        outputDataList.add(bytesToEncrypt);
        // Shuffle the tokens and save the shuffled tokens as the participant data
        Collections.sort(outputDataList, Convert.byteArrayComparator);
        if (isLast) {
            Set<Long> recipientAccounts = new HashSet<>(shuffling.getParticipantCount());
            for (byte[] publicKey : outputDataList) {
                if (!Crypto.isCanonicalPublicKey(publicKey) || !recipientAccounts.add(AccountService.getId(publicKey))) {
                    // duplicate or invalid recipient public key
                    log.debug("Invalid recipient public key " + Convert.toHexString(publicKey));
                    return new ShufflingRecipientsAttachment(shuffling.getId(), Convert.EMPTY_BYTES, shufflingStateHash);
                }
            }
            // last participant prepares ShufflingRecipients transaction instead of ShufflingProcessing
            return new ShufflingRecipientsAttachment(shuffling.getId(), outputDataList.toArray(new byte[outputDataList.size()][]),
                shufflingStateHash);
        } else {
            byte[] previous = null;
            for (byte[] decrypted : outputDataList) {
                if (previous != null && Arrays.equals(decrypted, previous)) {
                    log.debug("Duplicate decrypted data");
                    return new ShufflingProcessingAttachment(shuffling.getId(), Convert.EMPTY_BYTES, shufflingStateHash);
                }
                if (decrypted.length != 32 + 64 * (shuffling.getParticipantCount() - participantIndex - 1)) {
                    log.debug("Invalid encrypted data length in process " + decrypted.length);
                    return new ShufflingProcessingAttachment(shuffling.getId(), Convert.EMPTY_BYTES, shufflingStateHash);
                }
                previous = decrypted;
            }
            return new ShufflingProcessingAttachment(shuffling.getId(), outputDataList.toArray(new byte[outputDataList.size()][]),
                shufflingStateHash);
        }
    }

    @Override
    public ShufflingCancellationAttachment revealKeySeeds(Shuffling shuffling, final byte[] secretBytes, long cancellingAccountId, byte[] shufflingStateHash) {
        globalSync.readLock();
        try {
        Iterator<ShufflingParticipant> participants = getParticipants(shuffling.getId()).iterator();
            if (cancellingAccountId != shuffling.getAssigneeAccountId()) {
                throw new RuntimeException(String.format("Current shuffling cancellingAccountId %s does not match %s",
                    Long.toUnsignedString(shuffling.getAssigneeAccountId()), Long.toUnsignedString(cancellingAccountId)));
            }
            if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, getStageHash(shuffling))) {
                throw new RuntimeException("Current shuffling state hash does not match");
            }
            long accountId = AccountService.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretBytes)));
            byte[][] data = null;
            while (participants.hasNext()) {
                ShufflingParticipant participant = participants.next();
                if (participant.getAccountId() == accountId) {
                    data = getData(participant.getShufflingId(), participant.getAccountId());
                    break;
                }
            }
            if (!participants.hasNext()) {
                throw new RuntimeException("Last participant cannot have keySeeds to reveal");
            }
            if (data == null) {
                throw new RuntimeException("Account " + Long.toUnsignedString(accountId) + " has not submitted data");
            }
            final byte[] nonce = Convert.toBytes(shuffling.getId());
            final List<byte[]> keySeeds = new ArrayList<>();
            byte[] nextParticipantPublicKey = accountService.getPublicKeyByteArray(participants.next().getAccountId());
            byte[] keySeed = Crypto.getKeySeed(secretBytes, nextParticipantPublicKey, nonce);
            keySeeds.add(keySeed);
            byte[] publicKey = Crypto.getPublicKey(keySeed);
            byte[] decryptedBytes = null;
            // find the data that we encrypted
            for (byte[] bytes : data) {
                AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
                if (Arrays.equals(encryptedData.getPublicKey(), publicKey)) {
                    try {
                        decryptedBytes = encryptedData.decrypt(keySeed, nextParticipantPublicKey);
                        break;
                    } catch (Exception ignore) {
                    }
                }
            }
            if (decryptedBytes == null) {
                throw new RuntimeException("None of the encrypted data could be decrypted");
            }
            // decrypt all iteratively, adding the key seeds to the result
            while (participants.hasNext()) {
                nextParticipantPublicKey = accountService.getPublicKeyByteArray(participants.next().getAccountId());
                keySeed = Crypto.getKeySeed(secretBytes, nextParticipantPublicKey, nonce);
                keySeeds.add(keySeed);
                AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(decryptedBytes);
                decryptedBytes = encryptedData.decrypt(keySeed, nextParticipantPublicKey);
            }
            return new ShufflingCancellationAttachment(shuffling.getId(), data, keySeeds.toArray(new byte[keySeeds.size()][]),
                shufflingStateHash, cancellingAccountId);
        } finally {
            globalSync.readUnlock();
        }
    }


    @Override
    public void verify(Shuffling shuffling, long accountId) {
        ShufflingParticipant participant = getParticipant(shuffling.getId(), accountId);

        if(participant != null) {
            changeStatusToVerified(participant);
            if (getVerifiedCount(shuffling.getId()) == shuffling.getParticipantCount()) {
                distribute(shuffling);
            }
        }
    }

    @Override
    public void cancelBy(Shuffling shuffling, ShufflingParticipant participant, byte[][] blameData, byte[][] keySeeds) {
        changeStatusToCancel(participant, blameData, keySeeds);
        boolean startingBlame = shuffling.getStage() != ShufflingStage.BLAME;
        if (startingBlame) {
            setStage(shuffling, ShufflingStage.BLAME, participant.getAccountId(), (short) (blockchainConfig.getShufflingProcessingDeadline() + shuffling.getParticipantCount()));
        }
        updateHeightAndInsert(shuffling);
        if (startingBlame) {
            shufflingListeners.notify(shuffling, ShufflingEvent.SHUFFLING_BLAME_STARTED);
        }
        log.trace("Shuffling cancelBy {} entered stage {}, blamingAcc {}, remaining blocks {}",
            Long.toUnsignedString(shuffling.getId()), shuffling.getStage(), participant.getAccountId(), shuffling.getBlocksRemaining());
    }

    @Override
    public void cancel(Shuffling shuffling, Block block) {
        LedgerEvent event = LedgerEvent.SHUFFLING_CANCELLATION;
        long blamedAccountId = blame(shuffling);

        List<ShufflingParticipant> participants = getParticipants(shuffling.getId());
        for (ShufflingParticipant participant : participants) {
            Account participantAccount = accountService.getAccount(participant.getAccountId());
            shuffling.getHoldingType().addToUnconfirmedBalance(participantAccount, event, shuffling.getId(), shuffling.getHoldingId(), shuffling.getAmount());
            if (participantAccount.getId() != blamedAccountId) {
                if (shuffling.getHoldingType() != HoldingType.APL) {
                    accountService.addToUnconfirmedBalanceATM(participantAccount, event, shuffling.getId(), blockchainConfig.getShufflingDepositAtm());
                }
            } else {
                if (shuffling.getHoldingType() == HoldingType.APL) {
                    accountService.addToUnconfirmedBalanceATM(participantAccount, event, shuffling.getId(), -blockchainConfig.getShufflingDepositAtm());
                }
                accountService.addToBalanceATM(participantAccount, event, shuffling.getId(), -blockchainConfig.getShufflingDepositAtm());
            }
        }
        if (blamedAccountId != 0) {
            // as a penalty the deposit goes to the generators of the finish block and previous 3 blocks
            long fee = blockchainConfig.getShufflingDepositAtm() / 4;
            int shardHeight = blockchain.getShardInitialBlock().getHeight();
            long[] generators = null;
            for (int i = 0; i < 3; i++) {
                int blockHeight = block.getHeight() - i - 1;
                if (generators == null && shardHeight > blockHeight) {
                    generators = shardDao.getLastShard().getGeneratorIds();

                }
                Account previousGeneratorAccount;
                if (shardHeight > blockHeight) {
                    int diff = shardHeight - blockHeight - 1;
                    previousGeneratorAccount = accountService.getAccount(generators[diff]);
                } else {
                    previousGeneratorAccount = accountService.getAccount(blockchain.getBlockAtHeight(blockHeight).getGeneratorId());
                }
                accountService.addToBalanceAndUnconfirmedBalanceATM(previousGeneratorAccount, LedgerEvent.BLOCK_GENERATED, block.getId(), fee);
                accountService.addToForgedBalanceATM(previousGeneratorAccount, fee);
                log.debug("Shuffling penalty {} {} awarded to forger at height {}", ((double) fee) / blockchainConfig.getOneAPL(), blockchainConfig.getCoinSymbol(),
                    block.getHeight() - i - 1);
            }
            fee = blockchainConfig.getShufflingDepositAtm() - 3 * fee;
            Account blockGeneratorAccount = accountService.getAccount(block.getGeneratorId());
            accountService.addToBalanceAndUnconfirmedBalanceATM(blockGeneratorAccount, LedgerEvent.BLOCK_GENERATED, block.getId(), fee);
            accountService.addToForgedBalanceATM(blockGeneratorAccount, fee);
            log.debug("Shuffling penalty {} {} awarded to forger at height {}", ((double) fee) / blockchainConfig.getOneAPL(), blockchainConfig.getCoinSymbol(),
                block.getHeight());
        }
        setStage(shuffling, ShufflingStage.CANCELLED, blamedAccountId, (short) 0);
        updateHeightAndInsert(shuffling);
        shufflingListeners.notify(shuffling, ShufflingEvent.SHUFFLING_CANCELLED);
        if (deleteFinished) {
            log.debug("Deleting Shuffling Cancelled = {} , height = {} / {}", Long.toUnsignedString(shuffling.getId()), block.getHeight(), shuffling.getHeight());
            delete(shuffling);
        }
        log.debug("Shuffling {} was cancelled, blaming account {}", Long.toUnsignedString(shuffling.getId()), Long.toUnsignedString(blamedAccountId));
    }

    @Override
    public boolean isFull(Shuffling shuffling, Block block) {
        int transactionSize = Constants.MIN_TRANSACTION_SIZE; // min transaction size with no attachment
        if (shuffling.getStage() == ShufflingStage.REGISTRATION) {
            transactionSize += 1 + 32;
        } else { // must use same for PROCESSING/VERIFICATION/BLAME
            transactionSize = 16384; // max observed was 15647 for 30 participants
        }
        return block.getPayloadLength() + transactionSize > blockchainConfig.getCurrentConfig().getMaxPayloadLength();
    }

    @Override
    public void updateParticipantData(Shuffling shuffling, Transaction transaction, ShufflingProcessingAttachment attachment) {
        long participantId = transaction.getSenderId();
        byte[][] data = attachment.getData();
        ShufflingParticipant participant = getParticipant(shuffling.getId(), participantId);
        setData(participant, data, transaction.getTimestamp());
        changeStatusToProcessed(participant, transaction.getFullHash(), attachment.getHash());
        if (data != null && data.length == 0) {
            // couldn't decrypt all data from previous participants
            cancelBy(shuffling, participant);
            return;
        }
        shuffling.setAssigneeAccountId(participant.getNextAccountId());
        shuffling.setBlocksRemaining(blockchainConfig.getShufflingProcessingDeadline());
        updateHeightAndInsert(shuffling);
        shufflingListeners.notify(shuffling, ShufflingEvent.SHUFFLING_PROCESSING_ASSIGNED);
        log.trace("Shuffling updateParticipant {} entered stage {}, assignee {}, remaining blocks {}",
            Long.toUnsignedString(shuffling.getId()), shuffling.getStage(), Long.toUnsignedString(shuffling.getAssigneeAccountId()), shuffling.getBlocksRemaining());
    }

    @Override
    public void updateRecipients(Shuffling shuffling, Transaction transaction, ShufflingRecipientsAttachment attachment) {
        long participantId = transaction.getSenderId();
        shuffling.setRecipientPublicKeys(attachment.getRecipientPublicKeys());
        ShufflingParticipant participant = getParticipant(shuffling.getId(), participantId);
        changeStatusToProcessed(participant, transaction.getFullHash(), null);
        if (shuffling.getRecipientPublicKeys().length == 0) {
            // couldn't decrypt all data from previous participants
            cancelBy(shuffling, participant);
            return;
        }
        changeStatusToVerified(participant);
        // last participant announces all valid recipient public keys

        for (byte[] recipientPublicKey : shuffling.getRecipientPublicKeys()) {
            long recipientId = AccountService.getId(recipientPublicKey);
            if (accountPublicKeyService.setOrVerifyPublicKey(recipientId, recipientPublicKey)) {
                Account account = accountService.createAccount(recipientId);
                accountPublicKeyService.apply(account, recipientPublicKey);
            }
        }
        setStage(shuffling, ShufflingStage.VERIFICATION, 0, (short) (blockchainConfig.getShufflingProcessingDeadline() + shuffling.getParticipantCount()));
        updateHeightAndInsert(shuffling);
        shufflingListeners.notify(shuffling, ShufflingEvent.SHUFFLING_PROCESSING_FINISHED);
        log.trace("Shuffling updateRecipient {} entered stage {}, assignee {}, remaining blocks {}",
            Long.toUnsignedString(shuffling.getId()), shuffling.getStage(), Long.toUnsignedString(shuffling.getAssigneeAccountId()), shuffling.getBlocksRemaining());
    }

    @Override
    public void addParticipant(Shuffling shuffling, long participantId) {
        // Update the shuffling assignee to point to the new participant and update the next pointer of the existing participant
        // to the new participant
        ShufflingParticipant lastParticipant = getParticipant(shuffling.getId(), shuffling.getAssigneeAccountId());
        setNextAccountId(lastParticipant, participantId);
        addParticipant(shuffling.getId(), participantId, shuffling.getRegistrantCount());
        shuffling.setRegistrantCount((byte) (shuffling.getRegistrantCount() + 1));
        // Check if participant registration is complete and if so update the shuffling
        if (shuffling.getRegistrantCount() == shuffling.getParticipantCount()) {
            setStage(shuffling, ShufflingStage.PROCESSING, shuffling.getIssuerId(), blockchainConfig.getShufflingProcessingDeadline());
        } else {
            shuffling.setAssigneeAccountId(participantId);
        }
        updateHeightAndInsert(shuffling);
        if (shuffling.getStage() == ShufflingStage.PROCESSING) {
            shufflingListeners.notify(shuffling, ShufflingEvent.SHUFFLING_PROCESSING_ASSIGNED);
        }
        log.trace("Shuffling addParticipant {} entered stage {}, assignee {}, remaining blocks {}",
            Long.toUnsignedString(shuffling.getId()), shuffling.getStage(), Long.toUnsignedString(shuffling.getAssigneeAccountId()), shuffling.getBlocksRemaining());

    }

    @Override
    public int getHoldingShufflingCount(long holdingId, boolean includeFinished) {
        return shufflingRepository.getHoldingShufflingCount(holdingId, includeFinished);
    }

    @Override
    public List<Shuffling> getHoldingShufflings(long holdingId, ShufflingStage stage, boolean includeFinished, int from, int to) {
        return shufflingRepository.getHoldingShufflings(holdingId, stage, includeFinished, from, to);
    }

    @Override
    public List<Shuffling> getAccountShufflings(long accountId, boolean includeFinished, int from, int to) {
        return shufflingRepository.getAccountShufflings(accountId, includeFinished, from, to);
    }

    @Override
    public List<Shuffling> getAssignedShufflings(long assigneeAccountId, int from, int to) {
        return shufflingRepository.getAssignedShufflings(assigneeAccountId, from, to);
    }

    @Override
    public byte[] getParticipantsHash(Iterable<ShufflingParticipant> participants) {
        MessageDigest digest = Crypto.sha256();
        participants.forEach(participant -> digest.update(Convert.toBytes(participant.getAccountId())));
        return digest.digest();
    }

    @Override
    public byte[] getStageHash(Shuffling shuffling) {
        switch (shuffling.getStage()) {
            case REGISTRATION: {
                return getFullHash(shuffling.getId());
            }
            case PROCESSING: {
                if (shuffling.getAssigneeAccountId() == shuffling.getIssuerId()) {
                    List<ShufflingParticipant> participants = getParticipants(shuffling.getId());
                    return getParticipantsHash(participants);
                } else {
                    ShufflingParticipant participant = getParticipant(shuffling.getId(), shuffling.getAssigneeAccountId());
                    return getPreviousParticipant(participant).getDataTransactionFullHash();
                }
            }
            case VERIFICATION: {
                return getLastParticipant(shuffling.getId()).getDataTransactionFullHash();
            }
            case BLAME: {
                return getParticipant(shuffling.getId(), shuffling.getAssigneeAccountId()).getDataTransactionFullHash();
            }
            case CANCELLED: {
                byte[] hash = getLastParticipant(shuffling.getId()).getDataTransactionFullHash();
                if (hash != null && hash.length > 0) {
                    return hash;
                }
                List<ShufflingParticipant> participants = getParticipants(shuffling.getId());
                return getParticipantsHash(participants);
            }
            case DONE: {
                return getLastParticipant(shuffling.getId()).getDataTransactionFullHash();
            }

            default:
                throw new UnsupportedOperationException();
        }

    }

    private void setNextAccountId(ShufflingParticipant participant, long nextAccountId) {
        if (participant.getNextAccountId() != 0) {
            throw new IllegalStateException("nextAccountId already set to " + Long.toUnsignedString(participant.getNextAccountId()));
        }
        participant.setNextAccountId(nextAccountId);
        participant.setHeight(blockchain.getHeight());

        participantTable.insert(participant);
    }

    // caller must update database
    private void setStage(Shuffling shuffling, ShufflingStage stage, long assigneeAccountId, short blocksRemaining) {
        if (!shuffling.getStage().canBecome(stage)) {
            throw new IllegalStateException(String.format("Shuffling in stage %s cannot go to stage %s", shuffling.getStage(), stage));
        }
        if ((stage == ShufflingStage.VERIFICATION || stage == ShufflingStage.DONE) && assigneeAccountId != 0) {
            throw new IllegalArgumentException(String.format("Invalid assigneeAccountId %s for stage %s", Long.toUnsignedString(assigneeAccountId), stage));
        }
        if ((stage == ShufflingStage.REGISTRATION || stage == ShufflingStage.PROCESSING || stage == ShufflingStage.BLAME) && assigneeAccountId == 0) {
            throw new IllegalArgumentException(String.format("In stage %s assigneeAccountId cannot be 0", stage));
        }
        if ((stage == ShufflingStage.DONE || stage == ShufflingStage.CANCELLED) && blocksRemaining != 0) {
            throw new IllegalArgumentException(String.format("For stage %s remaining blocks cannot be %s", stage, blocksRemaining));
        }
        shuffling.setStage(stage);
        shuffling.setAssigneeAccountId(assigneeAccountId);
        shuffling.setBlocksRemaining(blocksRemaining);
        log.debug("Shuffling {} entered stage {}, assignee {}, remaining blocks {}",
            Long.toUnsignedString(shuffling.getId()), shuffling.getStage(), Long.toUnsignedString(shuffling.getAssigneeAccountId()), shuffling.getBlocksRemaining());
    }

    private void updateHeightAndInsert(Shuffling shuffling) {
        shuffling.setHeight(blockchain.getHeight());
        shufflingRepository.insert(shuffling);
    }

    private void delete(Shuffling shuffling) {
        List<ShufflingParticipant> participants = getParticipants(shuffling.getId());
        for (ShufflingParticipant participant : participants) {
            delete(participant);
        }
        shuffling.setHeight(blockchain.getHeight());
        shufflingRepository.delete(shuffling);
        log.debug("DELETED Shuffling {} entered stage {}, assignee {}, remaining blocks {}",
            Long.toUnsignedString(shuffling.getId()), shuffling.getStage(), Long.toUnsignedString(shuffling.getAssigneeAccountId()), shuffling.getBlocksRemaining());

    }

    private void cancelBy(Shuffling shuffling, ShufflingParticipant participant) {
        cancelBy(shuffling, participant, Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    }

    private void distribute(Shuffling shuffling) {
        if (shuffling.getRecipientPublicKeys().length != shuffling.getParticipantCount()) {
            cancelBy(shuffling, getLastParticipant(shuffling.getId()));
            return;
        }

        for (byte[] recipientPublicKey : shuffling.getRecipientPublicKeys()) {
            byte[] publicKey = accountService.getPublicKeyByteArray(AccountService.getId(recipientPublicKey));
            if (publicKey != null && !Arrays.equals(publicKey, recipientPublicKey)) {
                // distribution not possible, do a cancellation on behalf of last participant instead
                cancelBy(shuffling, getLastParticipant(shuffling.getId()));
                return;
            }
        }
        LedgerEvent event = LedgerEvent.SHUFFLING_DISTRIBUTION;
        List<ShufflingParticipant> participants = getParticipants(shuffling.getId());
        for (ShufflingParticipant participant : participants) {
            Account participantAccount = accountService.getAccount(participant.getAccountId());
            shuffling.getHoldingType().addToBalance(participantAccount, event, shuffling.getId(), shuffling.getHoldingId(), -shuffling.getAmount());
            if (shuffling.getHoldingType() != HoldingType.APL) {
                accountService.addToBalanceATM(participantAccount, event, shuffling.getId(), -blockchainConfig.getShufflingDepositAtm());
            }
        }
        for (byte[] recipientPublicKey : shuffling.getRecipientPublicKeys()) {
            long recipientId = AccountService.getId(recipientPublicKey);
            Account recipientAccount = accountService.createAccount(recipientId);
            accountPublicKeyService.apply(recipientAccount, recipientPublicKey);
            shuffling.getHoldingType().addToBalanceAndUnconfirmedBalance(recipientAccount, event, shuffling.getId(), shuffling.getHoldingId(), shuffling.getAmount());
            if (shuffling.getHoldingType() != HoldingType.APL) {
                accountService.addToBalanceAndUnconfirmedBalanceATM(recipientAccount, event, shuffling.getId(), blockchainConfig.getShufflingDepositAtm());
            }
        }
        setStage(shuffling, ShufflingStage.DONE, 0, (short) 0);
        updateHeightAndInsert(shuffling);
        shufflingListeners.notify(shuffling, ShufflingEvent.SHUFFLING_DONE);
        if (deleteFinished) {
            log.debug("Deleting Shuffling Done = {} , height = {}", Long.toUnsignedString(shuffling.getId()), shuffling.getHeight());
            delete(shuffling);
        }
        log.debug("Shuffling {} was distributed", Long.toUnsignedString(shuffling.getId()));
        log.trace("Shuffling distributed {} entered stage {}, assignee {}, remaining blocks {}",
            Long.toUnsignedString(shuffling.getId()), shuffling.getStage(), Long.toUnsignedString(shuffling.getAssigneeAccountId()), shuffling.getBlocksRemaining());
    }

    private long blame(Shuffling shuffling) {
        // if registration never completed, no one is to blame
        if (shuffling.getStage() == ShufflingStage.REGISTRATION) {
            log.debug("Registration never completed for shuffling {}", Long.toUnsignedString(shuffling.getId()));
            return 0;
        }
        // if no one submitted cancellation, blame the first one that did not submit processing data
        if (shuffling.getStage() == ShufflingStage.PROCESSING) {
            log.debug("Participant {} did not submit processing", Long.toUnsignedString(shuffling.getAssigneeAccountId()));
            return shuffling.getAssigneeAccountId();
        }
        List<ShufflingParticipant> participants = getParticipants(shuffling.getId());
        if (shuffling.getStage() == ShufflingStage.VERIFICATION) {
            // if verification started, blame the first one who did not submit verification
            for (ShufflingParticipant participant : participants) {
                if (participant.getState() != ShufflingParticipantState.VERIFIED) {
                    log.debug("Participant {} did not submit verification", Long.toUnsignedString(participant.getAccountId()));
                    return participant.getAccountId();
                }
            }
            throw new RuntimeException("All participants submitted data and verifications, blame phase should not have been entered");
        }
        Set<Long> recipientAccounts = new HashSet<>(shuffling.getParticipantCount());

        // start from issuer and verify all data up, skipping last participant
        for (int i = 0; i < shuffling.getParticipantCount() - 1; i++) {
            ShufflingParticipant participant = participants.get(i);
            byte[][] keySeeds = participant.getKeySeeds();
            // if participant couldn't submit key seeds because he also couldn't decrypt some of the previous data, this should have been caught before
            if (keySeeds.length == 0) {
                log.debug("Participant {} did not reveal keys", Long.toUnsignedString(participant.getAccountId()));
                return participant.getAccountId();
            }
            byte[] publicKey = Crypto.getPublicKey(keySeeds[0]);
            AnonymouslyEncryptedData encryptedData = null;
            for (byte[] bytes : participant.getBlameData()) {
                encryptedData = AnonymouslyEncryptedData.readEncryptedData(bytes);
                if (Arrays.equals(publicKey, encryptedData.getPublicKey())) {
                    // found the data that this participant encrypted
                    break;
                }
            }
            if (encryptedData == null || !Arrays.equals(publicKey, encryptedData.getPublicKey())) {
                // participant lied about key seeds or data
                log.debug("Participant {} did not submit blame data, or revealed invalid keys", Long.toUnsignedString(participant.getAccountId()));
                return participant.getAccountId();
            }
            for (int k = i + 1; k < shuffling.getParticipantCount(); k++) {
                ShufflingParticipant nextParticipant = participants.get(k);
                byte[] nextParticipantPublicKey = accountService.getPublicKeyByteArray(nextParticipant.getAccountId());
                byte[] keySeed = keySeeds[k - i - 1];
                byte[] participantBytes;
                try {
                    participantBytes = encryptedData.decrypt(keySeed, nextParticipantPublicKey);
                } catch (Exception e) {
                    // the next participant couldn't decrypt the data either, blame this one
                    log.debug("Could not decrypt data from participant {}", Long.toUnsignedString(participant.getAccountId()));
                    return participant.getAccountId();
                }
                boolean isLast = k == shuffling.getParticipantCount() - 1;
                if (isLast) {
                    // not encrypted data but plaintext recipient public key
                    if (!Crypto.isCanonicalPublicKey(publicKey)) {
                        // not a valid public key
                        log.debug("Participant {} submitted invalid recipient public key", Long.toUnsignedString(participant.getAccountId()));
                        return participant.getAccountId();
                    }
                    // check for collisions and assume they are intentional
                    byte[] currentPublicKey = accountService.getPublicKeyByteArray(AccountService.getId(participantBytes));
                    if (currentPublicKey != null && !Arrays.equals(currentPublicKey, participantBytes)) {
                        log.debug("Participant {} submitted colliding recipient public key", Long.toUnsignedString(participant.getAccountId()));
                        return participant.getAccountId();
                    }
                    if (!recipientAccounts.add(AccountService.getId(participantBytes))) {
                        log.debug("Participant {} submitted duplicate recipient public key", Long.toUnsignedString(participant.getAccountId()));
                        return participant.getAccountId();
                    }
                }
                if (nextParticipant.getState() == ShufflingParticipantState.CANCELLED && nextParticipant.getBlameData().length == 0) {
                    break;
                }
                boolean found = false;
                for (byte[] bytes : isLast ? shuffling.getRecipientPublicKeys() : nextParticipant.getBlameData()) {
                    if (Arrays.equals(participantBytes, bytes)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // the next participant did not include this participant's data
                    log.debug("Participant {} did not include previous data", Long.toUnsignedString(nextParticipant.getAccountId()));
                    return nextParticipant.getAccountId();
                }
                if (!isLast) {
                    encryptedData = AnonymouslyEncryptedData.readEncryptedData(participantBytes);
                }
            }
        }
        return shuffling.getAssigneeAccountId();
    }

}
