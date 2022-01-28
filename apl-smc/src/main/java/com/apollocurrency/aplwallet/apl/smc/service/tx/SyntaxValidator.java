/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.tx;

import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.smc.contract.vm.ResultValue;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.util.exception.ApiErrors.CONTRACT_METHOD_VALIDATION_ERROR;

/**
 * Parses but does not evaluate a given script by using the language specified in the config.
 * This validation process doesn't change the blockchain state.
 * This processor should be used during the state independent validation routine of the transaction
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class SyntaxValidator extends AbstractSmcContractTxProcessor {
    private final String script;

    public SyntaxValidator(String script, SmcContext context) {
        super(context);
        this.script = Objects.requireNonNull(script);
    }

    @Override
    public ResultValue process() {
        var result = ResultValue.UNDEFINED_RESULT;
        var isValid = machine.parse(script);
        if (!isValid) {
            getExecutionLog().setErrorCode(CONTRACT_METHOD_VALIDATION_ERROR.getErrorCode());
            result.setErrorCode(CONTRACT_METHOD_VALIDATION_ERROR.getErrorCode());
            result.setOutput(List.of(false));
            result.setErrorDescription(machine.getExecutionLog().toJsonString());
        }
        return result;
    }
}
