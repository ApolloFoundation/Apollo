/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.account.PhasingParamsDTO;
import com.apollocurrency.aplwallet.apl.core.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

import java.util.ArrayList;
import java.util.List;

public class PhasingParamsConverter implements Converter<PhasingParams, PhasingParamsDTO> {

    @Override
    public PhasingParamsDTO apply(PhasingParams model) {
        PhasingParamsDTO dto = new PhasingParamsDTO();
        dto.setQuorum(model.getQuorum());
        List<String> whiteListIds = new ArrayList<>();
        if (model.getWhitelist() != null) {
            for (long id : model.getWhitelist()) {
                whiteListIds.add(Long.toUnsignedString(id));
            }
        }
        dto.setWhitelist(whiteListIds);

        if (model.getVoteWeighting() != null) {
            dto.setPhasingMinBalance(model.getVoteWeighting().getMinBalance());
            dto.setPhasingMinBalanceModel(model.getVoteWeighting().getMinBalanceModel().getCode());
            dto.setPhasingHolding(Long.toUnsignedString(model.getVoteWeighting().getHoldingId()));
            dto.setPhasingVotingModel(model.getVoteWeighting().getVotingModel().getCode());
        }
        return dto;
    }

}
