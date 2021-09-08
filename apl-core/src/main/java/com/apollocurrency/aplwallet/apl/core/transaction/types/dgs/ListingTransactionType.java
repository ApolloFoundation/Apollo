/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.dgs;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsListing;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Search;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Map;
@Slf4j
@Singleton
public class ListingTransactionType extends DigitalGoodsTransactionType {

    @Inject
    public ListingTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, DGSService service) {
        super(blockchainConfig, accountService, service);
    }


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_LISTING;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.DIGITAL_GOODS_LISTING;
    }

    @Override
    public String getName() {
        return "DigitalGoodsListing";
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return getFeeFactory().createSizeBased(BigDecimal.valueOf(2), BigDecimal.valueOf(2), (tx, app) -> {
            DigitalGoodsListing attachment = (DigitalGoodsListing) tx.getAttachment();
            return attachment.getName().length() + attachment.getDescription().length();
        });
    }

    @Override
    public DigitalGoodsListing parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DigitalGoodsListing(buffer);
    }

    @Override
    public DigitalGoodsListing parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DigitalGoodsListing(attachmentData);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        DigitalGoodsListing attachment = (DigitalGoodsListing) transaction.getAttachment();
        if (attachment.getName().length() == 0 || attachment.getName().length() > Constants.MAX_DGS_LISTING_NAME_LENGTH
            || attachment.getDescription().length() > Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH
            || attachment.getTags().length() > Constants.MAX_DGS_LISTING_TAGS_LENGTH
            || attachment.getQuantity() < 0
            || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
            || attachment.getPriceATM() <= 0
            || attachment.getPriceATM() > getBlockchainConfig().getCurrentConfig().getMaxBalanceATM()) {
            throw new AplException.NotValidException("Invalid digital goods listing: " + attachment.getJSONObject());
        }
        PrunablePlainMessageAppendix prunablePlainMessage = transaction.getPrunablePlainMessage();
        if (prunablePlainMessage != null) {
            byte[] image = prunablePlainMessage.getMessage();
            if (image != null) {
                String mediaTypeName = Search.detectMimeType(image);
                String mediaType = mediaTypeName.substring(0, mediaTypeName.indexOf("/"));
                if (StringUtils.isBlank(mediaType) || !"image".equals(mediaType)) {
                    throw new AplException.NotValidException("Only image attachments allowed for DGS listing, media type is " + mediaType);
                }
            }
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DigitalGoodsListing attachment = (DigitalGoodsListing) transaction.getAttachment();
        dgsService.listGoods(transaction, attachment);
    }

    @Override
    public void doValidateAttachment(Transaction transaction) {

    }

    @Override
    public boolean isBlockDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return isDuplicate(TransactionTypes.TransactionTypeSpec.DGS_LISTING, getName(), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return true;
    }
}
