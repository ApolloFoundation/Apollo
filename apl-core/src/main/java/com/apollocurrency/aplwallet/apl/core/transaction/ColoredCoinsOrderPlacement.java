/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.monetary.Asset;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.util.AplException;

/**
 *
 * @author al
 */
abstract class ColoredCoinsOrderPlacement extends ColoredCoins {
    
    @Override
    public final void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsOrderPlacementAttachment attachment = (ColoredCoinsOrderPlacementAttachment) transaction.getAttachment();
        if (attachment.getPriceATM() <= 0 || attachment.getPriceATM() > blockchainConfig.getCurrentConfig().getMaxBalanceATM() || attachment.getAssetId() == 0) {
            throw new AplException.NotValidException("Invalid asset order placement: " + attachment.getJSONObject());
        }
        Asset asset = Asset.getAsset(attachment.getAssetId());
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
