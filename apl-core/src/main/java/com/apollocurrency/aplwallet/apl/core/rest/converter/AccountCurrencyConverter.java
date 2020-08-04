/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.account.AccountCurrencyDTO;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;

public class AccountCurrencyConverter implements Converter<AccountCurrency, AccountCurrencyDTO> {

    @Override
    public AccountCurrencyDTO apply(AccountCurrency model) {
        AccountCurrencyDTO dto = new AccountCurrencyDTO();

        dto.setAccount(Long.toUnsignedString(model.getAccountId()));
        dto.setAccountRS(Convert2.rsAccount(model.getAccountId()));
        dto.setCurrency(Long.toUnsignedString(model.getCurrencyId()));
        dto.setUnits(String.valueOf(model.getUnits()));
        dto.setUnconfirmedUnits(String.valueOf(model.getUnconfirmedUnits()));

        return dto;
    }

    public void addCurrency(AccountCurrencyDTO o, Currency model) {
        if (o != null && model != null) {
            o.setName(model.getName());
            o.setCode(model.getCode());
            o.setType(model.getType());
            o.setDecimals(model.getDecimals());
            o.setIssuanceHeight(model.getIssuanceHeight());
            o.setIssuerAccount(Long.toUnsignedString(model.getAccountId()));
            o.setIssuerAccountRS(Convert2.rsAccount(model.getAccountId()));
        }
    }
}
