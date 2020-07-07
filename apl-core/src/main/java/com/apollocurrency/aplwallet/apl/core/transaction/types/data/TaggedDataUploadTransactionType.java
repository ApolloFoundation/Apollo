/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.data;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataUploadAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
@Slf4j
class TaggedDataUploadTransactionType extends DataTransactionType {
    private final TimeService timeService;
    private final TaggedDataService taggedDataService;

    @Inject
    public TaggedDataUploadTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, TimeService timeService, TaggedDataService taggedDataService) {
        super(blockchainConfig, accountService);
        this.timeService = timeService;
        this.taggedDataService = taggedDataService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.TAGGED_DATA_UPLOAD;
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
        if (attachment.getData() == null && timeService.getEpochTime() - transaction.getTimestamp() < getBlockchainConfig().getMinPrunableLifetime()) {
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
        taggedDataService.add(transaction, attachment);
    }

    @Override
    public String getName() {
        return "TaggedDataUpload";
    }

    @Override
    public boolean isPruned(long transactionId) {
        return taggedDataService.isPruned(transactionId);
    }
}
