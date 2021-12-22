/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.converter;

import com.apollocurrency.aplwallet.api.v2.model.ContractMethod;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.smc.contract.SmartMethod;

import javax.inject.Singleton;
import java.math.BigInteger;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Singleton
public class SmartMethodMapper implements Converter<ContractMethod, SmartMethod> {

    @Override
    public SmartMethod apply(ContractMethod dto) {
        var modelBuilder = SmartMethod.builder()
            .name(dto.getFunction())
            .name(dto.getFunction())
            .value(BigInteger.ZERO);
        if (dto.getInput() != null) {
            modelBuilder.args(String.join(",", dto.getInput()));
        } else {
            modelBuilder.args("");
        }
        return modelBuilder.build();
    }

}
