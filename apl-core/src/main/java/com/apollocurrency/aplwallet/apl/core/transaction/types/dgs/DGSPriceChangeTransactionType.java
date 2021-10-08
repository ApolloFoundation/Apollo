/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.dgs;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DGSriceChangeAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class DGSPriceChangeTransactionType extends DGSTransactionType {

    @Inject
    public DGSPriceChangeTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, DGSService service) {
        super(blockchainConfig, accountService, service);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_CHANGE_PRICE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.DIGITAL_GOODS_PRICE_CHANGE;
    }

    @Override
    public String getName() {
        return "DigitalGoodsPriceChange";
    }

    @Override
    public DGSriceChangeAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DGSriceChangeAttachment(buffer);
    }

    @Override
    public DGSriceChangeAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DGSriceChangeAttachment(attachmentData);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        DGSriceChangeAttachment attachment = (DGSriceChangeAttachment) transaction.getAttachment();
        if (attachment.getPriceATM() <= 0
            || attachment.getPriceATM() > getBlockchainConfig().getCurrentConfig().getMaxBalanceATM()) {
            throw new AplException.NotValidException("Invalid digital goods price change: " + attachment.getJSONObject());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DGSriceChangeAttachment attachment = (DGSriceChangeAttachment) transaction.getAttachment();
        dgsService.changePrice(attachment.getGoodsId(), attachment.getPriceATM());
    }

    @Override
    public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
        DGSriceChangeAttachment attachment = (DGSriceChangeAttachment) transaction.getAttachment();
        DGSGoods goods = dgsService.getGoods(attachment.getGoodsId());
        if ((goods != null && transaction.getSenderId() != goods.getSellerId())) {
            throw new AplException.NotValidException("Invalid digital goods price change: " + attachment.getJSONObject());
        }
        if (goods == null || goods.isDelisted()) {
            throw new AplException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) + "not yet listed or already delisted");
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        DGSriceChangeAttachment attachment = (DGSriceChangeAttachment) transaction.getAttachment();
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
