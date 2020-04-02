/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.account.AccountAssetDTO;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.monetary.Asset;

public class AccountAssetConverter implements Converter<AccountAsset, AccountAssetDTO> {

    @Override
    public AccountAssetDTO apply(AccountAsset model) {
        AccountAssetDTO dto = new AccountAssetDTO();
        dto.setAccount(Long.toUnsignedString(model.getAccountId()));
        dto.setAssetId(model.getAssetId());
        dto.setAsset(Long.toUnsignedString(model.getAssetId()));
        dto.setQuantityATU(model.getQuantityATU());
        dto.setUnconfirmedQuantityATU(model.getUnconfirmedQuantityATU());

        return dto;
    }

    public void addAsset(AccountAssetDTO o, Asset model){
        if (o != null && model != null) {
            o.setName(model.getName());
            o.setDecimals(model.getDecimals());
        }

    }

    public void addAssetInfo(AccountAssetDTO o, Asset model){
        if (o != null && model != null) {
            o.setName(model.getName());
            o.setDescription(model.getDescription());
            o.setDecimals(model.getDecimals());
            o.setInitialQuantityATU(model.getInitialQuantityATU());
        }
    }

}
