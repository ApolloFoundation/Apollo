/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import java.util.List;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.api.dto.account.AccountControlPhasingDTO;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;

public class AccountControlPhasingConverter implements Converter<AccountControlPhasing, AccountControlPhasingDTO> {

    @Override
    public AccountControlPhasingDTO apply(AccountControlPhasing model) {
        AccountControlPhasingDTO dto = new AccountControlPhasingDTO();
        dto.setAccount(Long.toUnsignedString(model.getAccountId()));
        dto.setAccountRS(Convert2.rsAccount(model.getAccountId()));

        dto.setMaxDuration(model.getMaxDuration());
        dto.setMinDuration(model.getMinDuration());
        dto.setMaxFees(model.getMaxFees());
        dto.setQuorum(model.getPhasingParams() != null ? model.getPhasingParams().getQuorum() : 0);

        return dto;
    }

    @Override
    public AccountControlPhasingDTO convert(AccountControlPhasing model) {
        return apply(model);
    }

    @Override
    public List<AccountControlPhasingDTO> convert(List<AccountControlPhasing> models) {
        return models.stream().map(this).collect(Collectors.toList());
    }
}
