package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.model.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MandatoryTransaction implements Transaction {
    private Transaction transaction;
    private byte[] requiredTxHash;
    private Long dbId;

    public MandatoryTransaction(Transaction transaction, byte[] requiredTxHash, Long dbId) {
        this.transaction = transaction;
        this.requiredTxHash = requiredTxHash;
        this.dbId = dbId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MandatoryTransaction that = (MandatoryTransaction) o;
        return Objects.equals(transaction, that.transaction) &&
            Arrays.equals(requiredTxHash, that.requiredTxHash) &&
            Objects.equals(dbId, that.dbId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(transaction, dbId);
        result = 31 * result + Arrays.hashCode(requiredTxHash);
        return result;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public byte[] getRequiredTxHash() {
        return requiredTxHash;
    }

    @Override
    public boolean isUnconfirmedDuplicate(Map<TransactionType, Map<String, Integer>> unconfirmedDuplicates) {
        return transaction.isUnconfirmedDuplicate(unconfirmedDuplicates);
    }

    @Override
    public void sign(byte[] keySeed) {
        throw new RuntimeException("Transaction is already signed");
    }

    @Override
    public long getId() {
        return transaction.getId();
    }

    @Override
    public long getDbId() {
        throw new RuntimeException("DbId was not set for unconfirmed transaction");
    }

    @Override
    public String getStringId() {
        return transaction.getStringId();
    }

    @Override
    public long getSenderId() {
        return transaction.getSenderId();
    }

    @Override
    public byte[] getSenderPublicKey() {
        return transaction.getSenderPublicKey();
    }

    @Override
    public boolean shouldSavePublicKey() {
        return transaction.shouldSavePublicKey();
    }

    @Override
    public long getRecipientId() {
        return transaction.getRecipientId();
    }

    @Override
    public int getHeight() {
        return transaction.getHeight();
    }

    @Override
    public void setHeight(int height) {
        transaction.setHeight(height);
    }

    @Override
    public long getBlockId() {
        return transaction.getBlockId();
    }

    @Override
    public Block getBlock() {
        return transaction.getBlock();
    }

    @Override
    public void setBlock(Block block) {
        transaction.setBlock(block);
    }

    @Override
    public void unsetBlock() {
        transaction.unsetBlock();
    }

    @Override
    public short getIndex() {
        return transaction.getIndex();
    }

    @Override
    public void setIndex(int index) {
        transaction.setIndex(index);
    }

    @Override
    public int getTimestamp() {
        return transaction.getTimestamp();
    }

    @Override
    public int getBlockTimestamp() {
        return transaction.getBlockTimestamp();
    }

    @Override
    public short getDeadline() {
        return transaction.getDeadline();
    }

    @Override
    public int getExpiration() {
        return transaction.getExpiration();
    }

    @Override
    public long getAmountATM() {
        return transaction.getAmountATM();
    }

    @Override
    public long getFeeATM() {
        return transaction.getFeeATM();
    }

    @Override
    public void setFeeATM(long feeATM) {
        transaction.setFeeATM(feeATM);
    }

    @Override
    public String getReferencedTransactionFullHash() {
        return transaction.getReferencedTransactionFullHash();
    }

    @Override
    public byte[] referencedTransactionFullHash() {
        return transaction.referencedTransactionFullHash();
    }

    @Override
    public byte[] getSignature() {
        return transaction.getSignature();
    }

    @Override
    public String getFullHashString() {
        return transaction.getFullHashString();
    }

    @Override
    public byte[] getFullHash() {
        return transaction.getFullHash();
    }

    @Override
    public TransactionType getType() {
        return transaction.getType();
    }

    @Override
    public Attachment getAttachment() {
        return transaction.getAttachment();
    }

    @Override
    public boolean verifySignature() {
        return transaction.verifySignature();
    }

    @Override
    public boolean verifySignature(byte[][] publicKeys) {
        return transaction.verifySignature(publicKeys);
    }

    @Override
    public byte[] getBytes() {
        return transaction.getBytes();
    }

    @Override
    public byte[] getUnsignedBytes() {
        return transaction.getUnsignedBytes();
    }

    @Override
    public JSONObject getJSONObject() {
        return transaction.getJSONObject();
    }

    @Override
    public JSONObject getPrunableAttachmentJSON() {
        return transaction.getPrunableAttachmentJSON();
    }

    @Override
    public byte getVersion() {
        return transaction.getVersion();
    }

    @Override
    public int getFullSize() {
        return transaction.getFullSize();
    }

    @Override
    public MessageAppendix getMessage() {
        return transaction.getMessage();
    }

    @Override
    public EncryptedMessageAppendix getEncryptedMessage() {
        return transaction.getEncryptedMessage();
    }

    @Override
    public EncryptToSelfMessageAppendix getEncryptToSelfMessage() {
        return transaction.getEncryptToSelfMessage();
    }

    @Override
    public PhasingAppendix getPhasing() {
        return transaction.getPhasing();
    }

    @Override
    public boolean attachmentIsPhased() {
        return transaction.attachmentIsPhased();
    }

    @Override
    public PublicKeyAnnouncementAppendix getPublicKeyAnnouncement() {
        return transaction.getPublicKeyAnnouncement();
    }

    @Override
    public PrunablePlainMessageAppendix getPrunablePlainMessage() {
        return transaction.getPrunablePlainMessage();
    }

    @Override
    public boolean hasPrunablePlainMessage() {
        return transaction.hasPrunablePlainMessage();
    }

    @Override
    public PrunableEncryptedMessageAppendix getPrunableEncryptedMessage() {
        return transaction.getPrunableEncryptedMessage();
    }

    @Override
    public boolean hasPrunableEncryptedMessage() {
        return transaction.hasPrunableEncryptedMessage();
    }

    @Override
    public List<AbstractAppendix> getAppendages() {
        return transaction.getAppendages();
    }

    @Override
    public List<AbstractAppendix> getAppendages(boolean includeExpiredPrunable) {
        return transaction.getAppendages(includeExpiredPrunable);
    }

    @Override
    public List<AbstractAppendix> getAppendages(Filter<Appendix> filter, boolean includeExpiredPrunable) {
        return transaction.getAppendages(filter, includeExpiredPrunable);
    }

    @Override
    public int getECBlockHeight() {
        return transaction.getECBlockHeight();
    }

    @Override
    public long getECBlockId() {
        return transaction.getECBlockId();
    }

    /**
     * @deprecated see method with longer parameters list below
     */
    @Override
    public boolean attachmentIsDuplicate(Map<TransactionType, Map<String, Integer>> duplicates, boolean atAcceptanceHeight) {
        return transaction.attachmentIsDuplicate(duplicates, atAcceptanceHeight);
    }

    @Override
    public boolean attachmentIsDuplicate(Map<TransactionType, Map<String, Integer>> duplicates,
                                         boolean atAcceptanceHeight,
                                         Set<AccountControlType> senderAccountControls,
                                         AccountControlPhasing accountControlPhasing) {
        return transaction.attachmentIsDuplicate(duplicates, atAcceptanceHeight,
            senderAccountControls, accountControlPhasing);
    }

    public Long getDbEntryId() {
        return dbId;
    }
}
