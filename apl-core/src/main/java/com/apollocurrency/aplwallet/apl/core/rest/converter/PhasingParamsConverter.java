/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.account.PhasingParamsDTO;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingParams;

public class PhasingParamsConverter implements Converter<PhasingParams, PhasingParamsDTO> {

    private VoteWeightingConverter voteWeightingConverter = new VoteWeightingConverter();

    @Override
    public PhasingParamsDTO apply(PhasingParams model) {
        PhasingParamsDTO dto = new PhasingParamsDTO();
        dto.setQuorum(model.getQuorum());
        dto.setWhitelist( model.getWhitelist() != null ? model.getWhitelist()  : new long[]{});
        dto.setVoteWeighting(model.getVoteWeighting() != null ?
            voteWeightingConverter.apply(model.getVoteWeighting()) : null);
        return null;
    }

}
