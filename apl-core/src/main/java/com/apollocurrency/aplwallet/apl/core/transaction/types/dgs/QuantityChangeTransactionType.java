/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.dgs;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsQuantityChange;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class QuantityChangeTransactionType extends DigitalGoodsTransactionType {

    @Inject
    public QuantityChangeTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, DGSService service) {
        super(blockchainConfig, accountService, service);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_CHANGE_QUANTITY;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.DIGITAL_GOODS_QUANTITY_CHANGE;
    }

    @Override
    public String getName() {
        return "DigitalGoodsQuantityChange";
    }

    @Override
    public DigitalGoodsQuantityChange parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DigitalGoodsQuantityChange(buffer);
    }

    @Override
    public DigitalGoodsQuantityChange parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DigitalGoodsQuantityChange(attachmentData);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        DigitalGoodsQuantityChange attachment = (DigitalGoodsQuantityChange) transaction.getAttachment();
        if (attachment.getDeltaQuantity() < -Constants.MAX_DGS_LISTING_QUANTITY || attachment.getDeltaQuantity() > Constants.MAX_DGS_LISTING_QUANTITY) {
            throw new AplException.NotValidException("Invalid digital goods quantity change: " + attachment.getJSONObject());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DigitalGoodsQuantityChange attachment = (DigitalGoodsQuantityChange) transaction.getAttachment();
        dgsService.changeQuantity(attachment.getGoodsId(), attachment.getDeltaQuantity());
    }

    @Override
    public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
        DigitalGoodsQuantityChange attachment = (DigitalGoodsQuantityChange) transaction.getAttachment();
        DGSGoods goods = dgsService.getGoods(attachment.getGoodsId());
        if (goods != null && transaction.getSenderId() != goods.getSellerId()) {
            throw new AplException.NotValidException("Invalid digital goods quantity change: " + attachment.getJSONObject());
        }
        if (goods == null || goods.isDelisted()) {
            throw new AplException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) + "not yet listed or already delisted");
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        DigitalGoodsQuantityChange attachment = (DigitalGoodsQuantityChange) transaction.getAttachment();
        // not a bug, uniqueness is based on DigitalGoods.DELISTING
        return isDuplicate(TransactionTypes.TransactionTypeSpec.DGS_DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }
}
