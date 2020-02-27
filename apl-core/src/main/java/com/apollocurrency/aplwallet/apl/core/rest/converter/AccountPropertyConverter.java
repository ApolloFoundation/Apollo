/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.AccountPropertyDTO;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountProperty;

public class AccountPropertyConverter implements Converter<AccountProperty, AccountPropertyDTO> {

    @Override
    public AccountPropertyDTO apply(AccountProperty model) {
        AccountPropertyDTO dto = new AccountPropertyDTO();
        dto.setId(model.getId());
        dto.setRecipientId(model.getRecipientId());
        dto.setSetterId(model.getSetterId());
        dto.setProperty(model.getProperty());
        dto.setValue(model.getValue());
        return dto;
    }
}
