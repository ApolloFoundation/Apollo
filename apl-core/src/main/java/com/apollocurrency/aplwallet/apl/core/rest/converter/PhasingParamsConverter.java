/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import javax.inject.Singleton;

import com.apollocurrency.aplwallet.api.dto.account.PhasingParamsDTO;
import com.apollocurrency.aplwallet.apl.core.model.PhasingParams;

import java.util.ArrayList;
import java.util.List;

@Singleton
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
