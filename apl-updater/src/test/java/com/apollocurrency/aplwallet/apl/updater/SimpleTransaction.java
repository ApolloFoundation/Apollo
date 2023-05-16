/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SimpleTransaction implements Transaction {
    private long id;
    private int height;
    private TransactionType type;
    private Attachment attachment;

    public SimpleTransaction(long id, TransactionType type, int height) {
        this.id = id;
        this.type = type;
        this.height = height;
    }

    public SimpleTransaction(long id, TransactionType type) {
        this(id, type, 0);
    }

    public SimpleTransaction(Transaction tr) {
        this(tr.getId(), tr.getType(), tr.getHeight());
    }

    public void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }

    @Override
    public Transaction getTransactionImpl() {
        return this;
    }

    @Override
    public boolean isUnconfirmedDuplicate(Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> unconfirmedDuplicates) {
        return false;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getStringId() {
        return null;
    }

    @Override
    public long getSenderId() {
        return 0;
    }

    @Override
    public boolean hasValidSignature() {
        return true;
    }

    @Override
    public byte[] getSenderPublicKey() {
        return new byte[0];
    }

    @Override
    public long getRecipientId() {
        return 0;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
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
    public void setBlock(Block block) {
    }

    @Override
    public void unsetBlock() {
    }

    @Override
    public short getIndex() {
        return 0;
    }

    @Override
    public void setIndex(int index) {
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
        return 0;
    }

    @Override
    public long getFeeATM() {
        return 0;
    }

    @Override
    public void setFeeATM(long feeATM) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getReferencedTransactionFullHash() {
        return null;
    }

    @Override
    public byte[] referencedTransactionFullHash() {
        return new byte[]{};
    }

    @Override
    public Signature getSignature() {
        return null;
    }

    @Override
    public String getFullHashString() {
        return null;
    }

    @Override
    public byte[] getFullHash() {
        return new byte[]{};
    }

    @Override
    public TransactionType getType() {
        return type;
    }

    @Override
    public Attachment getAttachment() {
        return attachment;
    }

    @Override
    public byte getVersion() {
        return 0;
    }

    @Override
    public MessageAppendix getMessage() {
        return null;
    }

    @Override
    public EncryptedMessageAppendix getEncryptedMessage() {
        return null;
    }

    @Override
    public EncryptToSelfMessageAppendix getEncryptToSelfMessage() {
        return null;
    }

    @Override
    public PhasingAppendix getPhasing() {
        return null;
    }

    @Override
    public boolean attachmentIsPhased() {
        return false;
    }

    @Override
    public PublicKeyAnnouncementAppendix getPublicKeyAnnouncement() {
        return null;
    }

    @Override
    public boolean hasPrunablePlainMessage() {
        return false;
    }

    @Override
    public boolean hasPrunableEncryptedMessage() {
        return false;
    }

    @Override
    public PrunablePlainMessageAppendix getPrunablePlainMessage() {
        return null;
    }

    @Override
    public PrunableEncryptedMessageAppendix getPrunableEncryptedMessage() {
        return null;
    }

    @Override
    public List<AbstractAppendix> getAppendages() {
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

    @Override
    public boolean ofType(TransactionTypes.TransactionTypeSpec spec) {
        return false;
    }

    @Override
    public boolean isNotOfType(TransactionTypes.TransactionTypeSpec spec) {
        return false;
    }

    @Override
    public Optional<String> getErrorMessage() {
        return Optional.empty();
    }

    @Override
    public void fail(String message) {

    }

    @Override
    public boolean isFailed() {
        return false;
    }

    @Override
    public boolean canFailDuringExecution() {
        return false;
    }

    @Override
    public void resetFail() {

    }

}

