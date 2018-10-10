/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.*;
import com.apollocurrency.aplwallet.apl.Block;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Objects;

public class JSONTransaction implements Transaction {
    private Transaction transaction;
    private int numberOfConfirmations;

    public int getNumberOfConfirmations() {
        return numberOfConfirmations;
    }

    public void setNumberOfConfirmations(int numberOfConfirmations) {
        this.numberOfConfirmations = numberOfConfirmations;
    }

    public JSONTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public int getHeight() {return transaction.getHeight();}

    @Override
    public long getId() {return transaction.getId();}

    @Override
    public String getStringId() {return transaction.getStringId();}

    @Override
    public long getSenderId() {return transaction.getSenderId();}

    @Override
    public byte[] getSenderPublicKey() {return transaction.getSenderPublicKey();}

    @Override
    public long getRecipientId() {return transaction.getRecipientId();}

    @Override
    public long getBlockId() {return transaction.getBlockId();}

    @Override
    public Block getBlock() {return transaction.getBlock();}

    @Override
    public short getIndex() {return transaction.getIndex();}

    @Override
    public int getTimestamp() {return transaction.getTimestamp();}

    @Override
    public int getBlockTimestamp() {return transaction.getBlockTimestamp();}

    @Override
    public short getDeadline() {return transaction.getDeadline();}

    @Override
    public int getExpiration() {return transaction.getExpiration();}

    @Override
    public long getAmountATM() {return transaction.getAmountATM();}

    @Override
    public long getFeeATM() {return transaction.getFeeATM();}

    @Override
    public String getReferencedTransactionFullHash() {return transaction.getReferencedTransactionFullHash();}

    @Override
    public byte[] getSignature() {return transaction.getSignature();}

    @Override
    public String getFullHash() {return transaction.getFullHash();}

    @Override
    public TransactionType getType() {return transaction.getType();}

    @Override
    public Attachment getAttachment() {return transaction.getAttachment();}

    @Override
    public boolean verifySignature() {return transaction.verifySignature();}

    @Override
    public void validate() throws AplException.ValidationException {transaction.validate();}

    @Override
    public byte[] getBytes() {return transaction.getBytes();}

    @Override
    public byte[] getUnsignedBytes() {return transaction.getUnsignedBytes();}

    @Override
    public JSONObject getJSONObject() {return transaction.getJSONObject();}

    @Override
    public JSONObject getPrunableAttachmentJSON() {return transaction.getPrunableAttachmentJSON();}

    @Override
    public byte getVersion() {return transaction.getVersion();}

    @Override
    public int getFullSize() {return transaction.getFullSize();}

    @Override
    public Appendix.Message getMessage() {return transaction.getMessage();}

    @Override
    public Appendix.EncryptedMessage getEncryptedMessage() {return transaction.getEncryptedMessage();}

    @Override
    public Appendix.EncryptToSelfMessage getEncryptToSelfMessage() {return transaction.getEncryptToSelfMessage();}

    @Override
    public Appendix.Phasing getPhasing() {return transaction.getPhasing();}

    @Override
    public Appendix.PrunablePlainMessage getPrunablePlainMessage() {return transaction.getPrunablePlainMessage();}

    @Override
    public Appendix.PrunableEncryptedMessage getPrunableEncryptedMessage() {return transaction.getPrunableEncryptedMessage();}

    @Override
    public List<? extends Appendix> getAppendages() {return transaction.getAppendages();}

    @Override
    public List<? extends Appendix> getAppendages(boolean includeExpiredPrunable) {return transaction.getAppendages(includeExpiredPrunable);}

    @Override
    public List<? extends Appendix> getAppendages(Filter<Appendix> filter, boolean includeExpiredPrunable) {return transaction.getAppendages(filter, includeExpiredPrunable);}

    @Override
    public int getECBlockHeight() {return transaction.getECBlockHeight();}

    @Override
    public long getECBlockId() {return transaction.getECBlockId();}

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public BasicAccount getSender() {
        return new BasicAccount(transaction.getSenderId());
    }

    public BasicAccount getRecipient() {
        return new BasicAccount(transaction.getRecipientId());
    }

    public boolean isPrivate() {
        return getType() == TransactionType.Payment.PRIVATE;
    }

    public boolean isOwnedBy(String ownerAccount) {
        BasicAccount ownerBasicAccount = new BasicAccount(ownerAccount);
        return isOwnedBy(ownerBasicAccount);
    }
    public boolean isOwnedBy(BasicAccount ownerAccount) {
        return getRecipient().equals(ownerAccount) || getSender().equals(ownerAccount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JSONTransaction)) return false;
        JSONTransaction that = (JSONTransaction) o;
        return numberOfConfirmations == that.numberOfConfirmations &&
                Objects.equals(transaction, that.transaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transaction, numberOfConfirmations);
    }
}
