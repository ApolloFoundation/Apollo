/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.account.AccountRestrictions;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public class SimpleTransaction implements Transaction {
    private long id;
    private int height;
    private TransactionType type;
    private Attachment attachment;

    public void setId(long id) {
        this.id = id;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }

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

    @Override
    public long getBlockId() {
        return 0;
    }

    @Override
    public Block getBlock() {
        return null;
    }

    public void setBlock(Block block) {
    }

    public void unsetBlock() {
    }

    @Override
    public short getIndex() {
        return 0;
    }

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
    public String getReferencedTransactionFullHash() {
        return null;
    }

    @Override
    public byte[] referencedTransactionFullHash() {
        return new byte[]{};
    }

    @Override
    public byte[] getSignature() {
        return new byte[0];
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

    public byte[] bytes() {
        return new byte[0];
    }

    @Override
    public byte[] getUnsignedBytes() {
        return new byte[0];
    }

    @Override
    public JSONObject getJSONObject() {
        return new JSONObject();
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

    public boolean hasPrunablePlainMessage() {
        return false;
    }

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
    public List<AbstractAppendix> getAppendages(boolean includeExpiredPrunable) {
        return null;
    }

    @Override
    public List<AbstractAppendix> getAppendages(Filter<Appendix> filter, boolean includeExpiredPrunable) {
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

