/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.data;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataExtendAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Slf4j
@Singleton
public class TaggedDataExtendTransactionType extends DataTransactionType {
    public static final String NAME = "TaggedDataExtend";
    private final TaggedDataService taggedDataService;
    private final TimeService timeService;
    private final Blockchain blockchain;

    @Inject
    public TaggedDataExtendTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, TaggedDataService taggedDataService, TimeService timeService, Blockchain blockchain) {
        super(blockchainConfig, accountService);
        this.taggedDataService = taggedDataService;
        this.timeService = timeService;
        this.blockchain = blockchain;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.TAGGED_DATA_EXTEND;
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
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        TaggedDataExtendAttachment attachment = (TaggedDataExtendAttachment) transaction.getAttachment();
        BlockchainConfig blockchainConfig = getBlockchainConfig();
        if (!blockchain.hasTransaction(attachment.getTaggedDataId(), blockchain.getHeight())) {
            throw new AplException.NotCurrentlyValidException("No such tagged data upload " + Long.toUnsignedString(attachment.getTaggedDataId()));
        }
        TaggedData taggedData = taggedDataService.getData(attachment.getTaggedDataId());
        if (taggedData != null && taggedData.getTransactionTimestamp() > timeService.getEpochTime() + 6 * blockchainConfig.getMinPrunableLifetime()) {
            throw new AplException.NotCurrentlyValidException("Data already extended, timestamp is " + taggedData.getTransactionTimestamp());
        }
        // Transaction is validated and not failed, so that data should be present for at least a minPrunableLifetime
        validateDataExistence(transaction, attachment);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        TaggedDataExtendAttachment attachment = (TaggedDataExtendAttachment) transaction.getAttachment();
        log.trace("applyAttach taggedDataExtend, trId = {}, att = {}", transaction.getId(), attachment);
        taggedDataService.extend(transaction, attachment);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isPruned(long transactionId) {
        return false;
    }

    private void validateDataExistence(Transaction transaction, TaggedDataExtendAttachment attachment) throws AplException.NotCurrentlyValidException {
        if ((attachment.jsonIsPruned() || attachment.getData() == null) && timeService.getEpochTime() - transaction.getTimestamp() < getBlockchainConfig().getMinPrunableLifetime()) {
            throw new AplException.NotCurrentlyValidException("Data has been pruned prematurely");
        }
    }
}
