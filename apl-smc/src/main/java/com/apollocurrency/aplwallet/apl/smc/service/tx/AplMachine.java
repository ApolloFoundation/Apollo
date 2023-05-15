/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.tx;

import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.smc.contract.vm.BaseContractMachine;
import com.apollocurrency.smc.contract.vm.ExecutionLog;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class AplMachine extends BaseContractMachine {
    AplMachine(SmcContext context, ExecutionLog executionLog) {
        super(context.getLanguageContext(), context.getExecutionEnv(), executionLog, context.getIntegrator());
    }
}
