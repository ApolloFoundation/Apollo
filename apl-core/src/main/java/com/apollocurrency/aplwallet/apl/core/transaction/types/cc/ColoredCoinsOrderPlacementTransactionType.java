/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsOrderPlacementAttachment;

abstract class ColoredCoinsOrderPlacementTransactionType extends ColoredCoinsTransactionType {
    private final AssetService assetService;

    public ColoredCoinsOrderPlacementTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AssetService assetService) {
        super(blockchainConfig, accountService);
        this.assetService = assetService;
    }

    @Override
    public final void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsOrderPlacementAttachment attachment = (ColoredCoinsOrderPlacementAttachment) transaction.getAttachment();
        if (attachment.getPriceATM() <= 0 || attachment.getPriceATM() > getBlockchainConfig().getCurrentConfig().getMaxBalanceATM() || attachment.getAssetId() == 0) {
            throw new AplException.NotValidException("Invalid asset order placement: " + attachment.getJSONObject());
        }
        Asset asset = assetService.getAsset(attachment.getAssetId());
        if (attachment.getQuantityATU() <= 0 || (asset != null && attachment.getQuantityATU() > asset.getInitialQuantityATU())) {
            throw new AplException.NotValidException("Invalid asset order placement asset or quantity: " + attachment.getJSONObject());
        }
        if (asset == null) {
            throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) + " does not exist yet");
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
