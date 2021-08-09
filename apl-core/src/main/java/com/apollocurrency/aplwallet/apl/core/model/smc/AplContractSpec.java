/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model.smc;

import com.apollocurrency.smc.polyglot.Version;
import com.apollocurrency.smc.polyglot.lib.ContractSpec;
import lombok.Builder;
import lombok.Data;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Data
@Builder
public class AplContractSpec {
    private final String language;
    private final Version version;
    private final ContractSpec contractSpec;
    private final String content;
}
