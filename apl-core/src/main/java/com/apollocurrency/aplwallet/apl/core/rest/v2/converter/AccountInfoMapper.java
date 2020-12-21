/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.converter;

import com.apollocurrency.aplwallet.api.v2.model.AccountInfoResp;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;

import javax.inject.Singleton;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Singleton
public class AccountInfoMapper implements Converter<Account, AccountInfoResp> {
    @Override
    public AccountInfoResp apply(Account model) {
        AccountInfoResp dto = new AccountInfoResp();
        dto.setAccount(Convert2.rsAccount(model.getId()));
        if (model.isChild()) {
            dto.setParent(Convert2.rsAccount(model.getParentId()));
        }
        dto.setBalance(Long.toUnsignedString(model.getBalanceATM()));
        dto.setUnconfirmedbalance(Long.toUnsignedString(model.getUnconfirmedBalanceATM()));
        if (model.getPublicKey() != null && model.getPublicKey().getPublicKey() != null) {
            dto.setStatus(AccountInfoResp.StatusEnum.VERIFIED);
        } else {
            dto.setStatus(AccountInfoResp.StatusEnum.CREATED);
        }
        return dto;
    }
}
