/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.*;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONObject;

import java.util.List;

public class SimpleTransactionImpl implements Transaction {
    TransactionType type;
    long recipientId;
    long senderId;
    long feeATM;
    long amountATM;
    long height;

    public SimpleTransactionImpl(TransactionType type, long recipientId, long senderId, long feeATM, long amountATM, long height) {
        this.type = type;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.feeATM = feeATM;
        this.amountATM = amountATM;
        this.height = height;
    }

    public void setHeight(long height) {

        this.height = height;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public String getStringId() {
        return null;
    }

    @Override
    public long getSenderId() {
        return senderId;
    }

    @Override
    public byte[] getSenderPublicKey() {
        return new byte[0];
    }

    @Override
    public long getRecipientId() {
        return recipientId;
    }

    @Override
    public int getHeight() {
        return (int) height;
    }

    @Override
    public long getBlockId() {
        return 0;
    }

    @Override
    public Block getBlock() {
        return null;
    }

    @Override
    public short getIndex() {
        return 0;
    }

    @Override
    public int getTimestamp() {
        return 0;
    }

    @Override
    public int getBlockTimestamp() {
        return 0;
    }

    @Override
    public short getDeadline() {
        return 0;
    }

    @Override
    public int getExpiration() {
        return 0;
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
    public String getReferencedTransactionFullHash() {
        return null;
    }

    @Override
    public byte[] getSignature() {
        return new byte[0];
    }

    @Override
    public String getFullHash() {
        return null;
    }

    public SimpleTransactionImpl() {
    }

    public SimpleTransactionImpl(TransactionType type, long recipientId, long senderId, long feeATM, long amountATM) {
        this.type = type;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.feeATM = feeATM;
        this.amountATM = amountATM;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public void setRecipientId(long recipientId) {
        this.recipientId = recipientId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public void setFeeATM(long feeATM) {
        this.feeATM = feeATM;
    }

    public void setAmountATM(long amountATM) {
        this.amountATM = amountATM;
    }

    @Override
    public TransactionType getType() {
        return type;
    }

    @Override
    public Attachment getAttachment() {
        return null;
    }

    @Override
    public boolean verifySignature() {
        return false;
    }

    @Override
    public void validate() throws AplException.ValidationException {

    }

    @Override
    public byte[] getBytes() {
        return new byte[0];
    }

    @Override
    public byte[] getUnsignedBytes() {
        return new byte[0];
    }

    @Override
    public JSONObject getJSONObject() {
        return null;
    }

    @Override
    public JSONObject getPrunableAttachmentJSON() {
        return null;
    }

    @Override
    public byte getVersion() {
        return 0;
    }

    @Override
    public int getFullSize() {
        return 0;
    }

    @Override
    public Appendix.Message getMessage() {
        return null;
    }

    @Override
    public Appendix.EncryptedMessage getEncryptedMessage() {
        return null;
    }

    @Override
    public Appendix.EncryptToSelfMessage getEncryptToSelfMessage() {
        return null;
    }

    @Override
    public Appendix.Phasing getPhasing() {
        return null;
    }

    @Override
    public Appendix.PrunablePlainMessage getPrunablePlainMessage() {
        return null;
    }

    @Override
    public Appendix.PrunableEncryptedMessage getPrunableEncryptedMessage() {
        return null;
    }

    @Override
    public List<? extends Appendix> getAppendages() {
        return null;
    }

    @Override
    public List<? extends Appendix> getAppendages(boolean includeExpiredPrunable) {
        return null;
    }

    @Override
    public List<? extends Appendix> getAppendages(Filter<Appendix> filter, boolean includeExpiredPrunable) {
        return null;
    }

    @Override
    public int getECBlockHeight() {
        return 0;
    }

    @Override
    public long getECBlockId() {
        return 0;
    }
}
