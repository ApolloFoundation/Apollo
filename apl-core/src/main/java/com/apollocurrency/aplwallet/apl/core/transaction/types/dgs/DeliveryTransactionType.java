/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.dgs;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsDelivery;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class DeliveryTransactionType extends DigitalGoodsTransactionType {
    private final Fee DGS_DELIVERY_FEE = new Fee.SizeBasedFee(getBlockchainConfig().getOneAPL(), Math.multiplyExact(2, getBlockchainConfig().getOneAPL()), 32) {
        @Override
        public int getSize(Transaction transaction, Appendix appendage) {
            DigitalGoodsDelivery attachment = (DigitalGoodsDelivery) transaction.getAttachment();
            return attachment.getGoodsDataLength() - 16;
        }
    };

    @Inject
    public DeliveryTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, DGSService service) {
        super(blockchainConfig, accountService, service);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_DELIVERY;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.DIGITAL_GOODS_DELIVERY;
    }

    @Override
    public String getName() {
        return "DigitalGoodsDelivery";
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return DGS_DELIVERY_FEE;
    }

    @Override
    public DigitalGoodsDelivery parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DigitalGoodsDelivery(buffer);
    }

    @Override
    public DigitalGoodsDelivery parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        if (attachmentData.get("goodsData") == null) {
            throw new AplException.NotValidException("Unencrypted goodsData is not supported");
        }
        return new DigitalGoodsDelivery(attachmentData);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        DigitalGoodsDelivery attachment = (DigitalGoodsDelivery) transaction.getAttachment();
        if (attachment.getGoodsDataLength() > Constants.MAX_DGS_GOODS_LENGTH) {
            throw new AplException.NotValidException("Invalid digital goods delivery data length: " + attachment.getGoodsDataLength());
        }
        if (attachment.getGoods() != null) {
            if (attachment.getGoods().getData().length == 0 || attachment.getGoods().getNonce().length != 32) {
                throw new AplException.NotValidException("Invalid digital goods delivery: " + attachment.getJSONObject());
            }
        }
        if (attachment.getDiscountATM() < 0
            || attachment.getDiscountATM() > getBlockchainConfig().getCurrentConfig().getMaxBalanceATM()) {
            throw new AplException.NotValidException("Invalid digital goods delivery: " + attachment.getJSONObject());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DigitalGoodsDelivery attachment = (DigitalGoodsDelivery) transaction.getAttachment();
        dgsService.deliver(transaction, attachment);
    }

    @Override
    public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
        DigitalGoodsDelivery attachment = (DigitalGoodsDelivery) transaction.getAttachment();
        DGSPurchase purchase = dgsService.getPendingPurchase(attachment.getPurchaseId());
        if (purchase != null && (purchase.getBuyerId() != transaction.getRecipientId()
            || transaction.getSenderId() != purchase.getSellerId()
            || attachment.getDiscountATM() > Math.multiplyExact(purchase.getPriceATM(), (long) purchase.getQuantity()))) {
            throw new AplException.NotValidException("Invalid digital goods delivery: " + attachment.getJSONObject());
        }
        if (purchase == null || purchase.getEncryptedGoods() != null) {
            throw new AplException.NotCurrentlyValidException("Purchase does not exist yet, or already delivered: " + attachment.getJSONObject());
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        DigitalGoodsDelivery attachment = (DigitalGoodsDelivery) transaction.getAttachment();
        return isDuplicate(TransactionTypes.TransactionTypeSpec.DGS_DELIVERY, Long.toUnsignedString(attachment.getPurchaseId()), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }
}
