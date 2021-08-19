/*
 *  Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.MathUtils;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

abstract class CCOrderPlacementTransactionType extends CCTransactionType {
    private final AssetService assetService;

    public CCOrderPlacementTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AssetService assetService) {
        super(blockchainConfig, accountService);
        this.assetService = assetService;
    }

    @Override
    public final void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsOrderPlacementAttachment attachment = (ColoredCoinsOrderPlacementAttachment) transaction.getAttachment();
        Asset asset = assetService.getAsset(attachment.getAssetId());
        if (asset != null && attachment.getQuantityATU() > asset.getInitialQuantityATU()) {
            throw new AplException.NotValidException("Invalid asset order placement asset or quantity: " + attachment.getJSONObject());
        }
        if (asset == null) {
            throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) + " does not exist yet");
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsOrderPlacementAttachment attachment = (ColoredCoinsOrderPlacementAttachment) transaction.getAttachment();
        long maxBalanceATM = getBlockchainConfig().getCurrentConfig().getMaxBalanceATM();
        if (attachment.getPriceATM() <= 0
            || attachment.getQuantityATU() <= 0
            || attachment.getPriceATM() > maxBalanceATM
            || attachment.getAssetId() == 0) {
            throw new AplException.NotValidException("Invalid asset order placement: " + attachment.getJSONObject());
        }
        long orderTotalATM = MathUtils.safeMultiply(attachment.getQuantityATU(), attachment.getPriceATM(), transaction);
        if (orderTotalATM > maxBalanceATM) {
            throw new AplException.NotValidException("Order total in ATMs " + orderTotalATM
                + " is greater than max allowed: " + maxBalanceATM
                + ", asset=" + Long.toUnsignedString(attachment.getAssetId())  + ", quantity="
                + attachment.getQuantityATU() + ", price=" + attachment.getPriceATM());
        }
    }

    @Override
    public final boolean canHaveRecipient() {
        return false;
    }

    @Override
    public final boolean isPhasingSafe() {
        return true;
    }

}
