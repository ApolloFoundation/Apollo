/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.converter;

import com.apollocurrency.aplwallet.api.v2.model.ArgSpec;
import com.apollocurrency.aplwallet.api.v2.model.MethodSpec;
import com.apollocurrency.aplwallet.api.v2.model.PropertySpec;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.smc.polyglot.lib.ContractSpec;

import javax.inject.Singleton;
import java.util.Locale;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Singleton
public class MethodSpecMapper implements Converter<ContractSpec.Member, MethodSpec> {

    @Override
    public MethodSpec apply(ContractSpec.Member model) {
        var dto = new MethodSpec();
        dto.setName(model.getName());
        dto.setValue(model.getValue());
        dto.setStateMutability(model.getStateMutability().name().toLowerCase(Locale.ROOT));
        var inputs = dto.getInputs();
        if (model.getInputs() != null) {
            model.getInputs().forEach(item -> {
                var arg = new ArgSpec();
                arg.setName(item.getName());
                arg.setType(item.getType());
                inputs.add(arg);
            });
        }
        var outputs = dto.getOutputs();
        if (model.getOutputs() != null) {
            model.getOutputs().forEach(item -> {
                var rc = new PropertySpec();
                rc.setName(item.getName());
                rc.setType(item.getType());
                outputs.add(rc);
            });
        }
        return dto;
    }
}
