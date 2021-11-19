/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.crypto.AplIdGenerator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.SET_PHASING_ONLY;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionUtils.calculateFullHash;

@Slf4j
public class TransactionImpl implements Transaction {

    private final short deadline;
    private final long recipientId;
    private final long amountATM;
    private final byte[] referencedTransactionFullHash;
    private final TransactionType type;
    private final int ecBlockHeight;
    private final long ecBlockId;
    private final byte version;
    private final int timestamp;
    private final AbstractAttachment attachment;
    private final MessageAppendix message;
    private final EncryptedMessageAppendix encryptedMessage;
    private final EncryptToSelfMessageAppendix encryptToSelfMessage;
    private final PublicKeyAnnouncementAppendix publicKeyAnnouncement;
    private final PhasingAppendix phasing;
    private final PrunablePlainMessageAppendix prunablePlainMessage;
    private final PrunableEncryptedMessageAppendix prunableEncryptedMessage;
    private final List<AbstractAppendix> appendages;
    private final byte[] senderPublicKey;
    private volatile long feeATM; // remove final modifier to set fee outside the class TODO get back 'final' modifier
    private volatile Signature signature;
    private volatile int height;
    private volatile long blockId;
    private volatile Block block;
    private volatile int blockTimestamp;
    private volatile short index;
    private volatile long id;
    private volatile String stringId;
    private volatile long senderId;
    private volatile byte[] fullHash;
    private volatile String errorMessage;
    private volatile boolean hasValidSignature = false;

    TransactionImpl(BuilderImpl builder) {

        this.timestamp = builder.timestamp;
        this.deadline = builder.deadline;
        this.senderPublicKey = builder.senderPublicKey;
        this.recipientId = builder.recipientId;
        this.amountATM = builder.amountATM;
        this.referencedTransactionFullHash = builder.referencedTransactionFullHash;
        this.type = builder.type;
        this.version = builder.version;
        this.blockId = builder.blockId;
        this.height = builder.height;
        this.index = builder.index;
        this.id = builder.id;
        this.senderId = builder.senderId;
        this.blockTimestamp = builder.blockTimestamp;
        this.fullHash = builder.fullHash;
        this.ecBlockHeight = builder.ecBlockHeight;
        this.ecBlockId = builder.ecBlockId;
        this.feeATM = builder.feeATM;
        this.errorMessage = builder.errorMessage;
        List<AbstractAppendix> list = new ArrayList<>();
        if ((this.attachment = builder.attachment) != null) {
            list.add(this.attachment);
        }
        if ((this.message = builder.message) != null) {
            list.add(this.message);
        }
        if ((this.encryptedMessage = builder.encryptedMessage) != null) {
            list.add(this.encryptedMessage);
        }
        if ((this.publicKeyAnnouncement = builder.publicKeyAnnouncement) != null) {
            list.add(this.publicKeyAnnouncement);
        }
        if ((this.encryptToSelfMessage = builder.encryptToSelfMessage) != null) {
            list.add(this.encryptToSelfMessage);
        }
        if ((this.phasing = builder.phasing) != null) {
            list.add(this.phasing);
        }
        if ((this.prunablePlainMessage = builder.prunablePlainMessage) != null) {
            list.add(this.prunablePlainMessage);
        }
        if ((this.prunableEncryptedMessage = builder.prunableEncryptedMessage) != null) {
            list.add(this.prunableEncryptedMessage);
        }
        this.appendages = Collections.unmodifiableList(list);
        this.signature = builder.signature;
    }

    public final void sign(Signature signature, Result unsignedRawTransaction) {
        this.signature = signature;
        this.fullHash = calculateFullHash(unsignedRawTransaction.array(), signature.bytes());
        this.id = AplIdGenerator.TRANSACTION.getIdByHash(fullHash).longValue();
        this.stringId = Long.toUnsignedString(id);
    }

    @Override
    public Transaction getTransactionImpl() {
        return this;
    }

    @Override
    public short getDeadline() {
        return deadline;
    }

    @Override
    public long getRecipientId() {
        return recipientId;
    }

    @Override
    public long getAmountATM() {
        return amountATM;
    }

    @Override
    public long getFeeATM() {
        return feeATM;
    }

    @Override
    public void setFeeATM(long feeATM) {
        this.feeATM = feeATM;
    }

    public long[] getBackFees() {
        return type.getBackFees(this);
    }

    @Override
    public String getReferencedTransactionFullHash() {
        return Convert.toHexString(referencedTransactionFullHash);
    }

    @Override
    public byte[] referencedTransactionFullHash() {
        return referencedTransactionFullHash;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    @Override
    public TransactionType getType() {
        return type;
    }

    @Override
    public byte getVersion() {
        return version;
    }

    @Override
    public long getBlockId() {
        return blockId;
    }

    @Override
    public Block getBlock() {
        if (block == null || blockId == 0) {
            log.debug("Block was not fetched for tx " + getId());
        }
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
        this.blockId = block.getId();
        this.height = block.getHeight();
        this.blockTimestamp = block.getTimestamp();
    }

    public void unsetBlock() {
        this.block = null;
        this.blockId = 0;
        this.blockTimestamp = -1;
        this.index = -1;
        // must keep the height set, as transactions already having been included in a popped-off block before
        // get priority when sorted for inclusion in a new block
    }

    @Override
    public short getIndex() {
        if (index == -1) {
            throw new IllegalStateException("Transaction index has not been set");
        }
        return index;
    }

    public void setIndex(int index) {
        this.index = (short) index;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    @Override
    public int getExpiration() {
        return timestamp + deadline * 60;
    }

    @Override
    public AbstractAttachment getAttachment() {
        return attachment;
    }

    @Override
    public List<AbstractAppendix> getAppendages() {
        return appendages;
    }

    @Override
    public long getId() {
        if (id == 0) {
            throwSignaturePreConditionError("ID");
        }
        return id;
    }

    @Override
    public String getStringId() {
        if (stringId == null) {
            stringId = Long.toUnsignedString(getId());
        }
        return stringId;
    }

    @Override
    public byte[] getFullHash() {
        if (fullHash == null) {
            throwSignaturePreConditionError("FULL_HASH");
        }
        return fullHash;
    }

    @Override
    public String getFullHashString() {
        return Convert.toHexString(getFullHash());
    }


    @Override
    public long getSenderId() {
        if (senderId == 0) {
            senderId = AccountService.getId(getSenderPublicKey());
        }
        return senderId;
    }

    @Override
    public boolean hasValidSignature() {
        return signature != null && signature.isVerified();
    }

    @Override
    public String toString() {
        return String.valueOf(getId());
    }

    @Override
    public MessageAppendix getMessage() {
        return message;
    }

    @Override
    public EncryptedMessageAppendix getEncryptedMessage() {
        return encryptedMessage;
    }

    @Override
    public EncryptToSelfMessageAppendix getEncryptToSelfMessage() {
        return encryptToSelfMessage;
    }

    @Override
    public PhasingAppendix getPhasing() {
        return phasing;
    }

    public boolean attachmentIsPhased() {
        return attachment.isPhased(this);
    }

    @Override
    public PublicKeyAnnouncementAppendix getPublicKeyAnnouncement() {
        return publicKeyAnnouncement;
    }

    @Override
    public PrunablePlainMessageAppendix getPrunablePlainMessage() {
        return prunablePlainMessage;
    }

    @Override
    public boolean hasPrunablePlainMessage() {
        return prunablePlainMessage != null;
    }

    @Override
    public PrunableEncryptedMessageAppendix getPrunableEncryptedMessage() {
        return prunableEncryptedMessage;
    }

    @Override
    public boolean hasPrunableEncryptedMessage() {
        return prunableEncryptedMessage != null;
    }

    @Override
    public int getECBlockHeight() {
        return ecBlockHeight;
    }

    @Override
    public long getECBlockId() {
        return ecBlockId;
    }

    @Override
    public boolean ofType(TransactionTypes.TransactionTypeSpec spec) {
        return type.getSpec() == spec;
    }

    @Override
    public boolean isNotOfType(TransactionTypes.TransactionTypeSpec spec) {
        return !ofType(spec);
    }

    @Override
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    @Override
    public void fail(@NonNull String message) {
        this.errorMessage = message;
    }

    @Override
    public boolean isFailed() {
        return this.errorMessage != null;
    }

    @Override
    public boolean canFailDuringExecution() {
        return type.canFailDuringExecution();
    }

    @Override
    public void resetFail() {
        this.errorMessage = null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TransactionImpl && this.getId() == ((Transaction) o).getId();
    }

    @Override
    public byte[] getSenderPublicKey() {
        if (senderPublicKey == null) {
            throw new IllegalStateException("Sender public key is not set");
        }
        return senderPublicKey;
    }

    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    /**
     * @deprecated see method with longer parameters list below
     */
    @Override
    public boolean attachmentIsDuplicate(Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates, boolean atAcceptanceHeight) {
        if (!attachmentIsPhased() && !atAcceptanceHeight) {
            // can happen for phased transactions having non-phasable attachment
            return false;
        }
        if (atAcceptanceHeight) {
            // all are checked at acceptance height for block duplicates
            if (type.isBlockDuplicate(this, duplicates)) {
                return true;
            }
            // phased are not further checked at acceptance height
            if (attachmentIsPhased()) {
                return false;
            }
        }
        // non-phased at acceptance height, and phased at execution height
        return type.isDuplicate(this, duplicates);
    }

    @Override
    public boolean attachmentIsDuplicate(Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates,
                                         boolean atAcceptanceHeight,
                                         Set<AccountControlType> senderAccountControls,
                                         AccountControlPhasing accountControlPhasing) {
        if (!attachmentIsPhased() && !atAcceptanceHeight) {
            // can happen for phased transactions having non-phasable attachment
            return false;
        }
        if (atAcceptanceHeight) {
            // verify transaction uniqueness for PHASING_ONLY account within the block
            if (this.isBlockDuplicate(this, duplicates, senderAccountControls, accountControlPhasing)) {
                return true;
            }
            // all are checked at acceptance height for block duplicates
            if (type.isBlockDuplicate(this, duplicates)) {
                return true;
            }
            // phased are not further checked at acceptance height
            if (attachmentIsPhased()) {
                return false;
            }
        }
        // non-phased at acceptance height, and phased at execution height
        return type.isDuplicate(this, duplicates);
    }

    private boolean isBlockDuplicate(Transaction transaction,
                                     Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates,
                                     Set<AccountControlType> senderAccountControls,
                                     AccountControlPhasing accountControlPhasing) {
        return
            senderAccountControls.contains(AccountControlType.PHASING_ONLY)
                && (accountControlPhasing != null && accountControlPhasing.getMaxFees() != 0)
                && transaction.getType().getSpec() != SET_PHASING_ONLY
                && TransactionType.isDuplicate(SET_PHASING_ONLY,
                Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
    }

    @Override
    public boolean isUnconfirmedDuplicate(Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return type.isUnconfirmedDuplicate(this, duplicates);
    }

    private void throwSignaturePreConditionError(String field) {
        if (signature == null) {
            throw new IllegalStateException("Transaction is not signed yet");
        } else {
            throw new IllegalStateException("The transaction " + field + " is not set for signed transaction");
        }
    }

    public static final class BuilderImpl implements Builder {
        @Override
        public String toString() {
            return "BuilderImpl{" +
                "deadline=" + deadline +
                ", senderPublicKey=" + Convert.toHexString(senderPublicKey) +
                ", amountATM=" + amountATM +
                ", feeATM=" + feeATM +
                ", type=" + type +
                ", version=" + version +
                ", attachment=" + attachment +
                ", recipientId=" + recipientId +
                ", referencedTransactionFullHash=" + Convert.toHexString(referencedTransactionFullHash) +
                ", signature=" + signature +
                ", message=" + message +
                ", encryptedMessage=" + encryptedMessage +
                ", encryptToSelfMessage=" + encryptToSelfMessage +
                ", publicKeyAnnouncement=" + publicKeyAnnouncement +
                ", phasing=" + phasing +
                ", prunablePlainMessage=" + prunablePlainMessage +
                ", prunableEncryptedMessage=" + prunableEncryptedMessage +
                ", blockId=" + blockId +
                ", height=" + height +
                ", id=" + id +
                ", senderId=" + senderId +
                ", timestamp=" + timestamp +
                ", blockTimestamp=" + blockTimestamp +
                ", fullHash=" + Convert.toHexString(fullHash) +
                ", ecBlockSet=" + ecBlockSet +
                ", ecBlockHeight=" + ecBlockHeight +
                ", ecBlockId=" + ecBlockId +
                ", index=" + index +
                '}';
        }

        private final short deadline;
        private final byte[] senderPublicKey;
        private final long amountATM;
        private final long feeATM;
        private final TransactionType type;
        private final byte version;
        private AbstractAttachment attachment;

        private long recipientId;
        private byte[] referencedTransactionFullHash;
        private Signature signature;
        private MessageAppendix message;
        private EncryptedMessageAppendix encryptedMessage;
        private EncryptToSelfMessageAppendix encryptToSelfMessage;
        private PublicKeyAnnouncementAppendix publicKeyAnnouncement;
        private PhasingAppendix phasing;
        private PrunablePlainMessageAppendix prunablePlainMessage;
        private PrunableEncryptedMessageAppendix prunableEncryptedMessage;
        private long blockId;
        private int height = Integer.MAX_VALUE;
        private long id;
        private long senderId;
        private int timestamp;
        private int blockTimestamp = -1;
        private byte[] fullHash;
        private boolean ecBlockSet = false;
        private int ecBlockHeight;
        private long ecBlockId;
        private short index = -1;
        private String errorMessage;

        public BuilderImpl(byte version, byte[] senderPublicKey, long amountATM, long feeATM, short deadline,
                           AbstractAttachment attachment, int timestamp, TransactionType transactionType) {
            this.version = version;
            this.deadline = deadline;
            this.senderPublicKey = senderPublicKey;
            this.amountATM = amountATM;
            this.feeATM = feeATM;
            this.attachment = attachment;
            this.type = transactionType;
            if (timestamp < 0) {
                throw new IllegalArgumentException("Timestamp cannot be less than 0");
            }
            this.timestamp = timestamp;
        }

        @Override
        public TransactionImpl build() {
            if (!ecBlockSet) {
                throw new IllegalStateException("Ec block was not set for transaction");
            }
            return new TransactionImpl(this);
        }

        @Override
        public BuilderImpl recipientId(long recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        @Override
        public BuilderImpl referencedTransactionFullHash(String referencedTransactionFullHash) {
            this.referencedTransactionFullHash = Convert.parseHexString(referencedTransactionFullHash);
            return this;
        }

        @Override
        public BuilderImpl referencedTransactionFullHash(byte[] referencedTransactionFullHash) {
            this.referencedTransactionFullHash = referencedTransactionFullHash;
            return this;
        }

        public BuilderImpl appendix(AbstractAttachment attachment) {
            this.attachment = attachment;
            return this;
        }

        @Override
        public BuilderImpl appendix(MessageAppendix message) {
            this.message = message;
            return this;
        }

        @Override
        public BuilderImpl appendix(EncryptedMessageAppendix encryptedMessage) {
            this.encryptedMessage = encryptedMessage;
            return this;
        }

        @Override
        public BuilderImpl appendix(EncryptToSelfMessageAppendix encryptToSelfMessage) {
            this.encryptToSelfMessage = encryptToSelfMessage;
            return this;
        }

        @Override
        public BuilderImpl appendix(PublicKeyAnnouncementAppendix publicKeyAnnouncement) {
            this.publicKeyAnnouncement = publicKeyAnnouncement;
            return this;
        }

        @Override
        public BuilderImpl appendix(PrunablePlainMessageAppendix prunablePlainMessage) {
            this.prunablePlainMessage = prunablePlainMessage;
            return this;
        }

        @Override
        public BuilderImpl appendix(PrunableEncryptedMessageAppendix prunableEncryptedMessage) {
            this.prunableEncryptedMessage = prunableEncryptedMessage;
            return this;
        }

        @Override
        public BuilderImpl appendix(PhasingAppendix phasing) {
            this.phasing = phasing;
            return this;
        }

        @Override
        public BuilderImpl timestamp(int timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @Override
        public BuilderImpl ecBlockHeight(int height) {
            this.ecBlockHeight = height;
            this.ecBlockSet = true;
            return this;
        }

        @Override
        public BuilderImpl ecBlockData(EcBlockData ecBlockData) {
            this.ecBlockHeight = ecBlockData.getHeight();
            this.ecBlockId = ecBlockData.getId();
            this.ecBlockSet = true;
            return this;
        }

        @Override
        public BuilderImpl ecBlockId(long blockId) {
            this.ecBlockId = blockId;
            this.ecBlockSet = true;
            return this;
        }

        @Override
        public BuilderImpl id(long id) {
            this.id = id;
            return this;
        }

        @Override
        public BuilderImpl signature(Signature signature) {
            this.signature = signature;
            return this;
        }

        @Override
        public BuilderImpl blockId(long blockId) {
            this.blockId = blockId;
            return this;
        }

        @Override
        public BuilderImpl height(int height) {
            this.height = height;
            return this;
        }

        @Override
        public BuilderImpl senderId(long senderId) {
            this.senderId = senderId;
            return this;
        }

        @Override
        public BuilderImpl fullHash(byte[] fullHash) {
            this.fullHash = fullHash;
            return this;
        }

        @Override
        public BuilderImpl blockTimestamp(int blockTimestamp) {
            this.blockTimestamp = blockTimestamp;
            return this;
        }

        @Override
        public BuilderImpl index(short index) {
            this.index = index;
            return this;
        }

        @Override
        public BuilderImpl errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
    }
}
