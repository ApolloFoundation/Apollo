/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.service;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.shuffling.ShufflingEvent;
import com.apollocurrency.aplwallet.apl.core.shuffling.ShufflingEventBinding;
import com.apollocurrency.aplwallet.apl.core.shuffling.ShufflingEventType;
import com.apollocurrency.aplwallet.apl.core.shuffling.dao.InMemoryShufflingRepository;
import com.apollocurrency.aplwallet.apl.core.shuffling.dao.ShufflingTable;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCancellationAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCreation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingProcessingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRecipientsAttachment;
import com.apollocurrency.aplwallet.apl.crypto.AnonymouslyEncryptedData;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.Setter;
import org.slf4j.Logger;

import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShufflingServiceImpl implements ShufflingService {
    private static final Logger LOG = getLogger(Shuffling.class);

    private PropertiesHolder propertiesHolder;
    private BlockchainConfig blockchainConfig;
    private final boolean deleteFinished;
    private final boolean useInMemoryShufflingRepository;
    private Blockchain blockchain;
    private ShardDao shardDao;
    private GlobalSync globalSync;
    private ShufflingTable shufflingTable;
    private ShufflingParticipantService shufflingParticipantService;
    private Event<Shuffling> shufflingEvent;
    /**
     * Cache, which contains all shuffling table records (including not latest), in other words this cache represent inMemory shuffling table
     * Purpose: getActiveShufflings db call require at least 50-100ms, with cache we can shorten time to 10-20ms
     * This field is optional and will not be initialized, when property "apl.useInMemoryShufflingRepository" was set to 'false'
     */
    private InMemoryShufflingRepository inMemoryShufflingRepository;

    @Setter
    @Inject
    private Instance<InMemoryShufflingRepository> shufflingRepositoryBean;


    @Inject
    public ShufflingServiceImpl(PropertiesHolder propertiesHolder, BlockchainConfig blockchainConfig, Blockchain blockchain, ShardDao shardDao, GlobalSync globalSync, ShufflingTable shufflingTable, ShufflingParticipantService shufflingParticipantService, Event<Shuffling> shufflingEvent) {
        this.propertiesHolder = propertiesHolder;
        this.blockchainConfig = blockchainConfig;
        this.blockchain = blockchain;
        this.deleteFinished = this.propertiesHolder.getBooleanProperty("apl.deleteFinishedShufflings");
        this.useInMemoryShufflingRepository = this.propertiesHolder.getBooleanProperty("apl.useInMemoryShufflingRepository", true);
        this.shardDao = shardDao;
        this.globalSync = globalSync;
        this.shufflingTable = shufflingTable;
        this.shufflingParticipantService = shufflingParticipantService;
        this.shufflingEvent = shufflingEvent;
    }


    private String last3Stacktrace() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        return String.join("->", getStacktraceSpec(stackTraceElements[5]), getStacktraceSpec(stackTraceElements[4]), getStacktraceSpec(stackTraceElements[3]));
    }

    private String getStacktraceSpec(StackTraceElement element) {
        String className = element.getClassName();
        return className.substring(className.lastIndexOf(".") + 1) + "." + element.getMethodName();
    }


    @Override
    public int getCount() {
        return useInMemoryShufflingRepository ?
                inMemoryShufflingRepository.getCount()
                :shufflingTable.getCount();
    }

    @Override
    public int getActiveCount() {
        return useInMemoryShufflingRepository ?
                inMemoryShufflingRepository.getActiveCount()
                : shufflingTable.getActiveCount();
    }

    @Override
    public List<Shuffling> getAll(int from, int to) {
        return useInMemoryShufflingRepository ?
                inMemoryShufflingRepository.extractAll(from, to)
                : shufflingTable.extractAll(from, to);
    }

    @Override
    public List<Shuffling> getActiveShufflings(int from, int to) {
        return useInMemoryShufflingRepository ?
                inMemoryShufflingRepository.getActiveShufflings(from, to)
                : shufflingTable.getActiveShufflings(from, to);
    }

    @Override
    public List<Shuffling> getFinishedShufflings(int from, int to) {
        return useInMemoryShufflingRepository ?
                inMemoryShufflingRepository.getFinishedShufflings(from, to)
                : shufflingTable.getFinishedShufflings(from, to);
    }

    @Override
    public Shuffling getShuffling(long shufflingId) {
        return useInMemoryShufflingRepository ? inMemoryShufflingRepository.getCopy(shufflingId) : shufflingTable.get(shufflingId);
    }

    @Override
    public Shuffling getShuffling(byte[] fullHash) {
        long shufflingId = Convert.fullHashToId(fullHash);
        Shuffling shuffling = useInMemoryShufflingRepository ? inMemoryShufflingRepository.getCopy(shufflingId) : shufflingTable.get(shufflingId);
        if (shuffling != null && !Arrays.equals(getFullHash(shuffling), fullHash)) {
            LOG.debug("Shuffling with different hash {} but same id found for hash {}",
                    Convert.toHexString(getFullHash(shuffling)), Convert.toHexString(fullHash));
            return null;
        }
        return shuffling;
    }

    @Override
    public int getHoldingShufflingCount(long holdingId, boolean includeFinished) {
        return useInMemoryShufflingRepository ?
                inMemoryShufflingRepository.getHoldingShufflingCount(holdingId, includeFinished)
                : shufflingTable.getHoldingShufflingCount(holdingId, includeFinished);
    }

    @Override
    public List<Shuffling> getHoldingShufflings(long holdingId, Stage stage, boolean includeFinished, int from, int to) {
        return useInMemoryShufflingRepository ?
                inMemoryShufflingRepository.getHoldingShufflings(holdingId, stage, includeFinished, from, to)
                : shufflingTable.getHoldingShufflings(holdingId, stage, includeFinished, from, to);
    }


    @Override
    public List<Shuffling> getAssignedShufflings(long assigneeAccountId, int from, int to) {
        return useInMemoryShufflingRepository ?
                inMemoryShufflingRepository.getAssignedShufflings(assigneeAccountId, from, to)
                : shufflingTable.getAssignedShufflings(assigneeAccountId, from, to);
    }

    @Override
    public void addShuffling(Transaction transaction, ShufflingCreation attachment) {
        Shuffling shuffling = new Shuffling(transaction, attachment);
        insert(shuffling);
        shufflingParticipantService.addParticipant(shuffling.getId(), transaction.getSenderId(), 0);
        shufflingEvent.select(literal(ShufflingEventType.SHUFFLING_CREATED)).fire(shuffling);
    }

    private AnnotationLiteral<ShufflingEvent> literal(ShufflingEventType eventType) {
        return new ShufflingEventBinding() {
            @Override
            public ShufflingEventType value() {
                return eventType;
            }
        };
    }

    @PostConstruct
    void init() {

        if (useInMemoryShufflingRepository) {
            if (shufflingRepositoryBean.isResolvable()) {
                try {
                    inMemoryShufflingRepository = shufflingRepositoryBean.get();
                    inMemoryShufflingRepository.putAll(shufflingTable.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues());
                }
                catch (SQLException e) {
                    throw new RuntimeException("Unable to init shuffling cache ", e);
                }
            } else {
                throw new RuntimeException("Shuffling inmemory repository is not resolvable");
            }
        }
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        LOG.trace("Call shuffling observer at height {} ", block.getHeight());
        long startTime = System.currentTimeMillis();
        if (block.getTransactions().size() == blockchainConfig.getCurrentConfig().getMaxNumberOfTransactions()
                || block.getPayloadLength() > blockchainConfig.getCurrentConfig().getMaxPayloadLength() - Constants.MIN_TRANSACTION_SIZE) {
            LOG.trace("Block is full");
            return;
        }
        List<Shuffling> shufflings = new ArrayList<>();
        List<Shuffling> activeShufflings = useInMemoryShufflingRepository ? inMemoryShufflingRepository.getActiveShufflings(0, Integer.MAX_VALUE) : getActiveShufflings(0, -1);
        for (Shuffling shuffling : activeShufflings) {
            if (!isFull(shuffling, block)) {
                shufflings.add(shuffling);
            } else {
                LOG.trace("Skip shuffling {} - {} - {}", shuffling.getId(), shuffling.getStage(), block.getHeight());
            }
        }
        shufflings.forEach(shuffling -> {
            shuffling.setBlocksRemaining((short) (shuffling.getBlocksRemaining() - 1));
            if (shuffling.getBlocksRemaining() <= 0) {
                cancel(shuffling, block);
            } else {
                insert(shuffling);
            }
        });
        LOG.trace("Shuffling observer time: {}", System.currentTimeMillis() - startTime);
    }


    public void onBlockPopOff(@Observes @BlockEvent(BlockEventType.BLOCK_POPPED) Block block) {
        if (useInMemoryShufflingRepository) {
            inMemoryShufflingRepository.rollback(block.getHeight() - 1);
        }
    }

    public void onRescanBegin(@Observes @BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
        init();
    }


    // caller must update database
    private void setStage(Shuffling shuffling, Stage stage, long assigneeAccountId, short blocksRemaining) {
        if (!shuffling.getStage().canBecome(stage)) {
            throw new IllegalStateException(String.format("Shuffling in stage %s cannot go to stage %s", shuffling.getStage(), stage));
        }
        if ((stage == Stage.VERIFICATION || stage == Stage.DONE) && assigneeAccountId != 0) {
            throw new IllegalArgumentException(String.format("Invalid assigneeAccountId %s for stage %s", Long.toUnsignedString(assigneeAccountId), stage));
        }
        if ((stage == Stage.REGISTRATION || stage == Stage.PROCESSING || stage == Stage.BLAME) && assigneeAccountId == 0) {
            throw new IllegalArgumentException(String.format("In stage %s assigneeAccountId cannot be 0", stage));
        }
        if ((stage == Stage.DONE || stage == Stage.CANCELLED) && blocksRemaining != 0) {
            throw new IllegalArgumentException(String.format("For stage %s remaining blocks cannot be %s", stage, blocksRemaining));
        }
        shuffling.setStage(stage);
        shuffling.setAssigneeAccountId(assigneeAccountId);
        shuffling.setBlocksRemaining(blocksRemaining);
        LOG.debug("Shuffling {} entered stage {}, assignee {}, remaining blocks {}",
                Long.toUnsignedString(shuffling.getId()), shuffling.getStage(), Long.toUnsignedString(shuffling.getAssigneeAccountId()), shuffling.getBlocksRemaining());
    }

    private void insert(Shuffling shuffling) {
        shuffling.setHeight(blockchain.getHeight());
        if (useInMemoryShufflingRepository) {
            inMemoryShufflingRepository.insert(shuffling);
        }
        shufflingTable.insert(shuffling);
    }

    /*
     * Meaning of assigneeAccountId in each shuffling stage:
     *  REGISTRATION: last currently registered participant
     *  PROCESSING: next participant in turn to submit processing data
     *  VERIFICATION: 0, not assigned to anyone
     *  BLAME: the participant who initiated the blame phase
     *  CANCELLED: the participant who got blamed for the shuffling failure, if any
     *  DONE: 0, not assigned to anyone
     */
    private ShufflingParticipant getParticipant(long shufflingId, long accountId) {
        return shufflingParticipantService.getParticipant(shufflingId, accountId);
    }

    private ShufflingParticipant getLastParticipant(Shuffling shuffling) {
        return shufflingParticipantService.getLastParticipant(shuffling.getId());
    }

    @Override
    public byte[] getStateHash(Shuffling shuffling) {

        switch (shuffling.getStage()) {
            case REGISTRATION:
                return getFullHash(shuffling);
            case PROCESSING:
                if (shuffling.getAssigneeAccountId() == shuffling.getIssuerId()) {
                    try (DbIterator<ShufflingParticipant> participants = shufflingParticipantService.getParticipants(shuffling.getId())) {
                        return getParticipantsHash(participants);
                    }
                } else {
                    ShufflingParticipant participant = getParticipant(shuffling.getId(), shuffling.getAssigneeAccountId());
                    return shufflingParticipantService.getPreviousParticipant(participant).getDataTransactionFullHash();
                }
            case VERIFICATION:
                return getLastParticipant(shuffling).getDataTransactionFullHash();

            case BLAME:
                return getParticipant(shuffling.getId(), shuffling.getAssigneeAccountId()).getDataTransactionFullHash();
            case CANCELLED:
                byte[] hash = getLastParticipant(shuffling).getDataTransactionFullHash();
                if (hash != null && hash.length > 0) {
                    return hash;
                }
                try (DbIterator<ShufflingParticipant> participants = shufflingParticipantService.getParticipants(shuffling.getId())) {
                    return getParticipantsHash(participants);
                }
            case DONE:
                return getLastParticipant(shuffling).getDataTransactionFullHash();
            default:
                throw new IllegalStateException("New stage " + shuffling.getStage() + " was added, but state hash calculation missing");

        }
    }

    @Override
    public byte[] getFullHash(Shuffling shuffling) {
        return blockchain.getFullHash(shuffling.getId());
    }

    @Override
    public ShufflingAttachment process(Shuffling shuffling, final long accountId, final byte[] secretBytes, final byte[] recipientPublicKey) {
        byte[][] data = Convert.EMPTY_BYTES;
        byte[] shufflingStateHash = null;
        int participantIndex = 0;
        List<ShufflingParticipant> shufflingParticipants = new ArrayList<>();
        globalSync.readLock();
        // Read the participant list for the shuffling
        try (DbIterator<ShufflingParticipant> participants = shufflingParticipantService.getParticipants(shuffling.getId())) {
            for (ShufflingParticipant participant : participants) {
                shufflingParticipants.add(participant);
                if (participant.getNextAccountId() == accountId) {
                    data = shufflingParticipantService.getData(participant);
                    shufflingStateHash = participant.getDataTransactionFullHash();
                    participantIndex = shufflingParticipants.size();
                }
            }
            if (shufflingStateHash == null) {
                shufflingStateHash = getParticipantsHash(shufflingParticipants);
            }
        }
        finally {
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
            }
            catch (Exception e) {
                LOG.info("Decryption failed", e);
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
            byte[] participantPublicKey = Account.getPublicKey(participant.getAccountId());
            AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.encrypt(bytesToEncrypt, secretBytes, participantPublicKey, nonce);
            bytesToEncrypt = encryptedData.getBytes();
        }
        outputDataList.add(bytesToEncrypt);
        // Shuffle the tokens and save the shuffled tokens as the participant data
        Collections.sort(outputDataList, Convert.byteArrayComparator);
        if (isLast) {
            Set<Long> recipientAccounts = new HashSet<>(shuffling.getParticipantCount());
            for (byte[] publicKey : outputDataList) {
                if (!Crypto.isCanonicalPublicKey(publicKey) || !recipientAccounts.add(Account.getId(publicKey))) {
                    // duplicate or invalid recipient public key
                    LOG.debug("Invalid recipient public key " + Convert.toHexString(publicKey));
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
                    LOG.debug("Duplicate decrypted data");
                    return new ShufflingProcessingAttachment(shuffling.getId(), Convert.EMPTY_BYTES, shufflingStateHash);
                }
                if (decrypted.length != 32 + 64 * (shuffling.getParticipantCount() - participantIndex - 1)) {
                    LOG.debug("Invalid encrypted data length in process " + decrypted.length);
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
        try (DbIterator<ShufflingParticipant> participants = shufflingParticipantService.getParticipants(shuffling.getId())) {
            if (cancellingAccountId != shuffling.getAssigneeAccountId()) {
                throw new RuntimeException(String.format("Current shuffling cancellingAccountId %s does not match %s",
                        Long.toUnsignedString(shuffling.getAssigneeAccountId()), Long.toUnsignedString(cancellingAccountId)));
            }
            if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, getStateHash(shuffling))) {
                throw new RuntimeException("Current shuffling state hash does not match");
            }
            long accountId = Account.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretBytes)));
            byte[][] data = null;
            while (participants.hasNext()) {
                ShufflingParticipant participant = participants.next();
                if (participant.getAccountId() == accountId) {
                    data = shufflingParticipantService.getData(participant);
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
            byte[] nextParticipantPublicKey = Account.getPublicKey(participants.next().getAccountId());
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
                    }
                    catch (Exception ignore) {}
                }
            }
            if (decryptedBytes == null) {
                throw new RuntimeException("None of the encrypted data could be decrypted");
            }
            // decrypt all iteratively, adding the key seeds to the result
            while (participants.hasNext()) {
                nextParticipantPublicKey = Account.getPublicKey(participants.next().getAccountId());
                keySeed = Crypto.getKeySeed(secretBytes, nextParticipantPublicKey, nonce);
                keySeeds.add(keySeed);
                AnonymouslyEncryptedData encryptedData = AnonymouslyEncryptedData.readEncryptedData(decryptedBytes);
                decryptedBytes = encryptedData.decrypt(keySeed, nextParticipantPublicKey);
            }
            return new ShufflingCancellationAttachment(shuffling.getId(), data, keySeeds.toArray(new byte[keySeeds.size()][]),
                    shufflingStateHash, cancellingAccountId);
        }
        finally {
            globalSync.readUnlock();
        }
    }

    @Override
    public void addParticipant(Shuffling shuffling, long participantId) {
        // Update the shuffling assignee to point to the new participant and update the next pointer of the existing participant
        // to the new participant
        ShufflingParticipant lastParticipant = shufflingParticipantService.getParticipant(shuffling.getId(), shuffling.getAssigneeAccountId());
        shufflingParticipantService.setNextAccountId(lastParticipant, participantId);
        shufflingParticipantService.addParticipant(shuffling.getId(), participantId, shuffling.getRegistrantCount());
        shuffling.setRegistrantCount((byte) (shuffling.getRegistrantCount() + 1));
        // Check if participant registration is complete and if so update the shuffling
        if (shuffling.getRegistrantCount() == shuffling.getParticipantCount()) {
            setStage(shuffling, Stage.PROCESSING, shuffling.getIssuerId(), blockchainConfig.getShufflingProcessingDeadline());
        } else {
            shuffling.setAssigneeAccountId(participantId);
        }
        insert(shuffling);
        if (shuffling.getStage() == Stage.PROCESSING) {
            shufflingEvent.select(literal(ShufflingEventType.SHUFFLING_PROCESSING_ASSIGNED)).fire(shuffling);
        }
    }

    @Override
    public List<Shuffling> getAccountShufflings(long accountId, boolean includeFinished, int from, int to) {
        return shufflingTable.getAccountShufflings(accountId, includeFinished, from, to);
    }

    @Override
    public void updateParticipantData(Shuffling shuffling, Transaction transaction, ShufflingProcessingAttachment attachment) {
        long participantId = transaction.getSenderId();
        byte[][] data = attachment.getData();
        ShufflingParticipant participant = shufflingParticipantService.getParticipant(shuffling.getId(), participantId);
        shufflingParticipantService.setData(participant, data, transaction.getTimestamp());
        shufflingParticipantService.setProcessed(participant, transaction.getFullHash(), attachment.getHash());
        if (data != null && data.length == 0) {
            // couldn't decrypt all data from previous participants
            cancelBy(shuffling, participant);
            return;
        }
        shuffling.setAssigneeAccountId(participant.getNextAccountId());
        shuffling.setBlocksRemaining(blockchainConfig.getShufflingProcessingDeadline());
        insert(shuffling);
        shufflingEvent.select(literal(ShufflingEventType.SHUFFLING_PROCESSING_ASSIGNED)).fire(shuffling);
    }

    @Override
    public void updateRecipients(Shuffling shuffling, Transaction transaction, ShufflingRecipientsAttachment attachment) {
        long participantId = transaction.getSenderId();
        shuffling.setRecipientPublicKeys(attachment.getRecipientPublicKeys());
        ShufflingParticipant participant = shufflingParticipantService.getParticipant(shuffling.getId(), participantId);
        shufflingParticipantService.setProcessed(participant, transaction.getFullHash(), null);
        if (shuffling.getRecipientPublicKeys().length == 0) {
            // couldn't decrypt all data from previous participants
            cancelBy(shuffling, participant);
            return;
        }
        shufflingParticipantService.verify(participant);
        // last participant announces all valid recipient public keys
        for (byte[] recipientPublicKey : shuffling.getRecipientPublicKeys()) {
            long recipientId = Account.getId(recipientPublicKey);
            if (Account.setOrVerify(recipientId, recipientPublicKey)) {
                Account.addOrGetAccount(recipientId).apply(recipientPublicKey);
            }
        }
        setStage(shuffling, Stage.VERIFICATION, 0, (short) (blockchainConfig.getShufflingProcessingDeadline() + shuffling.getParticipantCount()));
        insert(shuffling);
        shufflingEvent.select(literal(ShufflingEventType.SHUFFLING_PROCESSING_FINISHED)).fire(shuffling);
    }

    @Override
    public void verify(Shuffling shuffling, long accountId) {
        ShufflingParticipant participant = shufflingParticipantService.getParticipant(shuffling.getId(), accountId);
        shufflingParticipantService.verify(participant);
        int verifiedCount = shufflingParticipantService.getVerifiedCount(shuffling.getId());
        if (verifiedCount == shuffling.getParticipantCount()) {
            distribute(shuffling);
        }
    }

    @Override
    public void cancelBy(Shuffling shuffling, ShufflingParticipant participant, byte[][] blameData, byte[][] keySeeds) {
        shufflingParticipantService.cancel(participant, blameData, keySeeds);
        boolean startingBlame = shuffling.getStage() != Stage.BLAME;
        if (startingBlame) {
            setStage(shuffling, Stage.BLAME, participant.getAccountId(), (short) (blockchainConfig.getShufflingProcessingDeadline() + shuffling.getParticipantCount()));
        }
        insert(shuffling);
        if (startingBlame) {
            shufflingEvent.select(literal(ShufflingEventType.SHUFFLING_BLAME_STARTED)).fire(shuffling);
        }
    }

    private void cancelBy(Shuffling shuffling, ShufflingParticipant participant) {
        cancelBy(shuffling, participant, Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    }

    private void distribute(Shuffling shuffling) {
        byte[][] recipientPublicKeys = shuffling.getRecipientPublicKeys();
        if (recipientPublicKeys.length != shuffling.getParticipantCount()) {
            cancelBy(shuffling, getLastParticipant(shuffling));
            return;
        }
        for (byte[] recipientPublicKey : recipientPublicKeys) {
            byte[] publicKey = Account.getPublicKey(Account.getId(recipientPublicKey));
            if (publicKey != null && !Arrays.equals(publicKey, recipientPublicKey)) {
                // distribution not possible, do a cancellation on behalf of last participant instead
                cancelBy(shuffling, getLastParticipant(shuffling));
                return;
            }
        }
        LedgerEvent event = LedgerEvent.SHUFFLING_DISTRIBUTION;
        HoldingType holdingType = shuffling.getHoldingType();
        try (DbIterator<ShufflingParticipant> participants = shufflingParticipantService.getParticipants(shuffling.getId())) {
            for (ShufflingParticipant participant : participants) {
                Account participantAccount = Account.getAccount(participant.getAccountId());
                holdingType.addToBalance(participantAccount, event, shuffling.getId(), shuffling.getHoldingId(), -shuffling.getAmount());
                if (holdingType != HoldingType.APL) {
                    participantAccount.addToBalanceATM(event, shuffling.getId(), -blockchainConfig.getShufflingDepositAtm());
                }
            }
        }
        for (byte[] recipientPublicKey : recipientPublicKeys) {
            long recipientId = Account.getId(recipientPublicKey);
            Account recipientAccount = Account.addOrGetAccount(recipientId);
            recipientAccount.apply(recipientPublicKey);
            holdingType.addToBalanceAndUnconfirmedBalance(recipientAccount, event, shuffling.getId(), shuffling.getHoldingId(), shuffling.getAmount());
            if (holdingType != HoldingType.APL) {
                recipientAccount.addToBalanceAndUnconfirmedBalanceATM(event, shuffling.getId(), blockchainConfig.getShufflingDepositAtm());
            }
        }
        setStage(shuffling, Stage.DONE, 0, (short) 0);
        insert(shuffling);
        shufflingEvent.select(literal(ShufflingEventType.SHUFFLING_DONE)).fire(shuffling);
        if (deleteFinished) {
            delete(shuffling);
        }
        LOG.debug("Shuffling {} was distributed", Long.toUnsignedString(shuffling.getId()));
    }

    private void cancel(Shuffling shuffling, Block block) {
        LedgerEvent event = LedgerEvent.SHUFFLING_CANCELLATION;
        long blamedAccountId = blame(shuffling);
        try (DbIterator<ShufflingParticipant> participants = shufflingParticipantService.getParticipants(shuffling.getId())) {
            for (ShufflingParticipant participant : participants) {
                Account participantAccount = Account.getAccount(participant.getAccountId());
                HoldingType holdingType = shuffling.getHoldingType();
                holdingType.addToUnconfirmedBalance(participantAccount, event, shuffling.getId(), shuffling.getHoldingId(), shuffling.getAmount());
                if (participantAccount.getId() != blamedAccountId) {
                    if (holdingType != HoldingType.APL) {
                        participantAccount.addToUnconfirmedBalanceATM(event, shuffling.getId(), blockchainConfig.getShufflingDepositAtm());
                    }
                } else {
                    if (holdingType == HoldingType.APL) {
                        participantAccount.addToUnconfirmedBalanceATM(event, shuffling.getId(), -blockchainConfig.getShufflingDepositAtm());
                    }
                    participantAccount.addToBalanceATM(event, shuffling.getId(), -blockchainConfig.getShufflingDepositAtm());
                }
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
                    previousGeneratorAccount = Account.getAccount(generators[diff]);
                } else {
                    previousGeneratorAccount = Account.getAccount(blockchain.getBlockAtHeight(blockHeight).getGeneratorId());
                }
                previousGeneratorAccount.addToBalanceAndUnconfirmedBalanceATM(LedgerEvent.BLOCK_GENERATED, block.getId(), fee);
                previousGeneratorAccount.addToForgedBalanceATM(fee);
                LOG.debug("Shuffling penalty {} {} awarded to forger at height {}", ((double) fee) / Constants.ONE_APL, blockchainConfig.getCoinSymbol(),
                        block.getHeight() - i - 1);
            }
            fee = blockchainConfig.getShufflingDepositAtm() - 3 * fee;
            Account blockGeneratorAccount = Account.getAccount(block.getGeneratorId());
            blockGeneratorAccount.addToBalanceAndUnconfirmedBalanceATM(LedgerEvent.BLOCK_GENERATED, block.getId(), fee);
            blockGeneratorAccount.addToForgedBalanceATM(fee);
            LOG.debug("Shuffling penalty {} {} awarded to forger at height {}", ((double) fee) / Constants.ONE_APL, blockchainConfig.getCoinSymbol(),
                    block.getHeight());
        }
        setStage(shuffling, Stage.CANCELLED, blamedAccountId, (short) 0);
        insert(shuffling);
        shufflingEvent.select(literal(ShufflingEventType.SHUFFLING_CANCELLED)).fire(shuffling);
        if (deleteFinished) {
            delete(shuffling);
        }
        LOG.debug("Shuffling {} was cancelled, blaming account {}", Long.toUnsignedString(shuffling.getId()), Long.toUnsignedString(blamedAccountId));
    }

    private long blame(Shuffling shuffling) {
        // if registration never completed, no one is to blame
        Stage currentStage = shuffling.getStage();
        if (currentStage == Stage.REGISTRATION) {
            LOG.debug("Registration never completed for shuffling {}", Long.toUnsignedString(shuffling.getId()));
            return 0;
        }
        // if no one submitted cancellation, blame the first one that did not submit processing data
        if (currentStage == Stage.PROCESSING) {
            long assigneeAccountId = shuffling.getAssigneeAccountId();
            LOG.debug("Participant {} did not submit processing", Long.toUnsignedString(assigneeAccountId));
            return assigneeAccountId;
        }
        List<ShufflingParticipant> participants = new ArrayList<>();
        try (DbIterator<ShufflingParticipant> iterator = shufflingParticipantService.getParticipants(shuffling.getId())) {
            while (iterator.hasNext()) {
                participants.add(iterator.next());
            }
        }
        if (currentStage == Stage.VERIFICATION) {
            // if verification started, blame the first one who did not submit verification
            for (ShufflingParticipant participant : participants) {
                if (participant.getState() != ShufflingParticipantService.State.VERIFIED) {
                    LOG.debug("Participant {} did not submit verification", Long.toUnsignedString(participant.getAccountId()));
                    return participant.getAccountId();
                }
            }
            throw new RuntimeException("All participants submitted data and verifications, blame phase should not have been entered");
        }
        byte participantCount = shuffling.getParticipantCount();
        Set<Long> recipientAccounts = new HashSet<>(participantCount);
        // start from issuer and verify all data up, skipping last participant
        for (int i = 0; i < participantCount - 1; i++) {
            ShufflingParticipant participant = participants.get(i);
            byte[][] keySeeds = participant.getKeySeeds();
            // if participant couldn't submit key seeds because he also couldn't decrypt some of the previous data, this should have been caught before
            if (keySeeds.length == 0) {
                LOG.debug("Participant {} did not reveal keys", Long.toUnsignedString(participant.getAccountId()));
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
                LOG.debug("Participant {} did not submit blame data, or revealed invalid keys", Long.toUnsignedString(participant.getAccountId()));
                return participant.getAccountId();
            }
            for (int k = i + 1; k < participantCount; k++) {
                ShufflingParticipant nextParticipant = participants.get(k);
                byte[] nextParticipantPublicKey = Account.getPublicKey(nextParticipant.getAccountId());
                byte[] keySeed = keySeeds[k - i - 1];
                byte[] participantBytes;
                try {
                    participantBytes = encryptedData.decrypt(keySeed, nextParticipantPublicKey);
                }
                catch (Exception e) {
                    // the next participant couldn't decrypt the data either, blame this one
                    LOG.debug("Could not decrypt data from participant {}", Long.toUnsignedString(participant.getAccountId()));
                    return participant.getAccountId();
                }
                boolean isLast = k == participantCount - 1;
                if (isLast) {
                    // not encrypted data but plaintext recipient public key
                    if (!Crypto.isCanonicalPublicKey(publicKey)) {
                        // not a valid public key
                        LOG.debug("Participant {} submitted invalid recipient public key", Long.toUnsignedString(participant.getAccountId()));
                        return participant.getAccountId();
                    }
                    // check for collisions and assume they are intentional
                    byte[] currentPublicKey = Account.getPublicKey(Account.getId(participantBytes));
                    if (currentPublicKey != null && !Arrays.equals(currentPublicKey, participantBytes)) {
                        LOG.debug("Participant {} submitted colliding recipient public key", Long.toUnsignedString(participant.getAccountId()));
                        return participant.getAccountId();
                    }
                    if (!recipientAccounts.add(Account.getId(participantBytes))) {
                        LOG.debug("Participant {} submitted duplicate recipient public key", Long.toUnsignedString(participant.getAccountId()));
                        return participant.getAccountId();
                    }
                }
                if (nextParticipant.getState() == ShufflingParticipantService.State.CANCELLED && nextParticipant.getBlameData().length == 0) {
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
                    LOG.debug("Participant {} did not include previous data", Long.toUnsignedString(nextParticipant.getAccountId()));
                    return nextParticipant.getAccountId();
                }
                if (!isLast) {
                    encryptedData = AnonymouslyEncryptedData.readEncryptedData(participantBytes);
                }
            }
        }
        return shuffling.getAssigneeAccountId();
    }

    private void delete(Shuffling shuffling) {
        try (DbIterator<ShufflingParticipant> participants = shufflingParticipantService.getParticipants(shuffling.getId())) {
            for (ShufflingParticipant participant : participants) {
                shufflingParticipantService.delete(participant);
            }
        }
        shuffling.setHeight(blockchain.getHeight());
        if (useInMemoryShufflingRepository) {
            inMemoryShufflingRepository.delete(shuffling);
        }
        shufflingTable.delete(shuffling);
    }

    private boolean isFull(Shuffling shuffling, Block block) {
        int transactionSize = Constants.MIN_TRANSACTION_SIZE; // min transaction size with no attachment
        if (shuffling.getStage() == Stage.REGISTRATION) {
            transactionSize += 1 + 32;
        } else { // must use same for PROCESSING/VERIFICATION/BLAME
            transactionSize = 16384; // max observed was 15647 for 30 participants
        }
        return block.getPayloadLength() + transactionSize > blockchainConfig.getCurrentConfig().getMaxPayloadLength();
    }

    private byte[] getParticipantsHash(Iterable<ShufflingParticipant> participants) {
        MessageDigest digest = Crypto.sha256();
        participants.forEach(participant -> digest.update(Convert.toBytes(participant.getAccountId())));
        return digest.digest();
    }

}
