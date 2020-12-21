/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.account.AccountControlPhasingDTO;
import com.apollocurrency.aplwallet.apl.core.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;

import java.util.ArrayList;
import java.util.List;

public class AccountControlPhasingConverter implements Converter<AccountControlPhasing, AccountControlPhasingDTO> {

//    private PhasingParamsConverter phasingParamsConverter = new PhasingParamsConverter(); // not needed now

    @Override
    public AccountControlPhasingDTO apply(AccountControlPhasing model) {
        AccountControlPhasingDTO dto = new AccountControlPhasingDTO();
        dto.setAccount(Long.toUnsignedString(model.getAccountId()));
        dto.setAccountRS(Convert2.rsAccount(model.getAccountId()));

        dto.setMaxDuration(model.getMaxDuration());
        dto.setMinDuration(model.getMinDuration());
        dto.setMaxFees(model.getMaxFees());
        dto.setQuorum(model.getPhasingParams() != null ? model.getPhasingParams().getQuorum() : 0);
//        dto.setPhasingParams(model.getPhasingParams() != null ?
//            phasingParamsConverter.apply(model.getPhasingParams()) : null);
        if (model.getPhasingParams() != null
            && model.getPhasingParams().getWhitelist() != null
            && model.getPhasingParams().getWhitelist().length > 0) {
            List<Long> whileList = new ArrayList<>(model.getPhasingParams().getWhitelist().length);
            for (int i = 0; i < model.getPhasingParams().getWhitelist().length; i++) {
                long accountId = model.getPhasingParams().getWhitelist()[i];
                whileList.add(accountId);
            }
            dto.setWhitelist(whileList);
        }
        return dto;
    }

    @Override
    public AccountControlPhasingDTO convert(AccountControlPhasing model) {
        return apply(model);
    }

}
