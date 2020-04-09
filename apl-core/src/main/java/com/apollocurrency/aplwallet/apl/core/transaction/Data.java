/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.tagged.TaggedDataService;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedData;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataExtendAttachment;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataUploadAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;

/**
 *
 * @author al
 */
@Slf4j
public abstract class Data extends TransactionType {

    private static TaggedDataService taggedDataService;

    private static TaggedDataService lookupTaggedDataService() {
        if (taggedDataService == null) {
            taggedDataService = CDI.current().select(TaggedDataService.class).get();
        }
        return taggedDataService;
    }

    private static final Fee TAGGED_DATA_FEE = new Fee.SizeBasedFee(Constants.ONE_APL, Constants.ONE_APL / 10) {
        @Override
        public int getSize(Transaction transaction, Appendix appendix) {
            return appendix.getFullSize();
        }
    };

    private Data() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_DATA;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return TAGGED_DATA_FEE;
    }

    @Override
    public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public final boolean canHaveRecipient() {
        return false;
    }

    @Override
    public final boolean isPhasingSafe() {
        return false;
    }

    @Override
    public final boolean isPhasable() {
        return false;
    }

    public static final TransactionType TAGGED_DATA_UPLOAD = new Data() {
        @Override
        public byte getSubtype() {
            return SUBTYPE_DATA_TAGGED_DATA_UPLOAD;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.TAGGED_DATA_UPLOAD;
        }

        @Override
        public TaggedDataUploadAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new TaggedDataUploadAttachment(buffer);
        }

        @Override
        public TaggedDataUploadAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new TaggedDataUploadAttachment(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            TaggedDataUploadAttachment attachment = (TaggedDataUploadAttachment) transaction.getAttachment();
            if (attachment.getData() == null && Data.lookupTimeService().getEpochTime() - transaction.getTimestamp() < Data.lookupBlockchainConfig().getMinPrunableLifetime()) {
                throw new AplException.NotCurrentlyValidException("Data has been pruned prematurely");
            }
            if (attachment.getData() != null) {
                if (attachment.getName().length() == 0 || attachment.getName().length() > Constants.MAX_TAGGED_DATA_NAME_LENGTH) {
                    throw new AplException.NotValidException("Invalid name length: " + attachment.getName().length());
                }
                if (attachment.getDescription().length() > Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH) {
                    throw new AplException.NotValidException("Invalid description length: " + attachment.getDescription().length());
                }
                if (attachment.getTags().length() > Constants.MAX_TAGGED_DATA_TAGS_LENGTH) {
                    throw new AplException.NotValidException("Invalid tags length: " + attachment.getTags().length());
                }
                if (attachment.getType().length() > Constants.MAX_TAGGED_DATA_TYPE_LENGTH) {
                    throw new AplException.NotValidException("Invalid type length: " + attachment.getType().length());
                }
                if (attachment.getChannel().length() > Constants.MAX_TAGGED_DATA_CHANNEL_LENGTH) {
                    throw new AplException.NotValidException("Invalid channel length: " + attachment.getChannel().length());
                }
                if (attachment.getFilename().length() > Constants.MAX_TAGGED_DATA_FILENAME_LENGTH) {
                    throw new AplException.NotValidException("Invalid filename length: " + attachment.getFilename().length());
                }
                if (attachment.getData().length == 0 || attachment.getData().length > Constants.MAX_TAGGED_DATA_DATA_LENGTH) {
                    throw new AplException.NotValidException("Invalid data length: " + attachment.getData().length);
                }
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            TaggedDataUploadAttachment attachment = (TaggedDataUploadAttachment) transaction.getAttachment();
            log.trace("applyAttach taggedDataUpload, trId = {}, att = {}", transaction.getId(), attachment);
            lookupTaggedDataService().add(transaction, attachment);
        }

        @Override
        public String getName() {
            return "TaggedDataUpload";
        }

        @Override
        public boolean isPruned(long transactionId) {
            return lookupTaggedDataService().isPruned(transactionId);
        }
    };

    public static final TransactionType TAGGED_DATA_EXTEND = new Data() {
        @Override
        public byte getSubtype() {
            return SUBTYPE_DATA_TAGGED_DATA_EXTEND;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.TAGGED_DATA_EXTEND;
        }

        @Override
        public TaggedDataExtendAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new TaggedDataExtendAttachment(buffer);
        }

        @Override
        public TaggedDataExtendAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new TaggedDataExtendAttachment(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            TaggedDataExtendAttachment attachment = (TaggedDataExtendAttachment) transaction.getAttachment();
            if ((attachment.jsonIsPruned() || attachment.getData() == null) && Data.lookupTimeService().getEpochTime() - transaction.getTimestamp() < Data.lookupBlockchainConfig().getMinPrunableLifetime()) {
                throw new AplException.NotCurrentlyValidException("Data has been pruned prematurely");
            }
            if (!lookupBlockchain().hasTransaction(attachment.getTaggedDataId(), lookupBlockchain().getHeight())) {
                throw new AplException.NotCurrentlyValidException("No such tagged data upload " + Long.toUnsignedString(attachment.getTaggedDataId()));
            }
            TaggedData taggedData = lookupTaggedDataService().getData(attachment.getTaggedDataId());
            if (taggedData != null && taggedData.getTransactionTimestamp() > Data.lookupTimeService().getEpochTime() + 6 * lookupBlockchainConfig().getMinPrunableLifetime()) {
                throw new AplException.NotCurrentlyValidException("Data already extended, timestamp is " + taggedData.getTransactionTimestamp());
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            TaggedDataExtendAttachment attachment = (TaggedDataExtendAttachment) transaction.getAttachment();
            log.trace("applyAttach taggedDataExtend, trId = {}, att = {}", transaction.getId(), attachment);
            lookupTaggedDataService().extend(transaction, attachment);
        }

        @Override
        public String getName() {
            return "TaggedDataExtend";
        }

        @Override
        public boolean isPruned(long transactionId) {
            return false;
        }
    };

}
