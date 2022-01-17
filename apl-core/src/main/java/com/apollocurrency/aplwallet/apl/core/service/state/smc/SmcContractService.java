/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.api.v2.model.ContractMethod;
import com.apollocurrency.aplwallet.api.v2.model.ContractSpecResponse;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractSpec;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.ResultValue;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.polyglot.Version;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcContractService {

    /**
     * Returns the list of ASR module names.
     *
     * @param language the language name
     * @param version  the language version
     * @param type     the module type: token, escrow etc.
     * @return the list of ASR module names.
     */
    List<String> getAsrModules(String language, Version version, String type);

    List<ResultValue> processAllViewMethods(Address contractAddress, List<ContractMethod> members, ExecutionLog executionLog);

    /**
     * Returns the list of inherited ASR modules.
     *
     * @param asrModuleName the ASR module name
     * @param language      the language name
     * @param version       the language version
     * @return the list of ASR module names.
     */
    List<String> getInheritedAsrModules(String asrModuleName, String language, Version version);

    /**
     * Load the contract specification of the specified ASR module
     *
     * @param asrModuleName the ASR module name
     * @param language      the language name
     * @param version       the language version
     * @return the contract specification of the specified ASR module
     */
    AplContractSpec loadAsrModuleSpec(String asrModuleName, String language, Version version);

    /**
     * Load the contract specification of the specified contract address.
     *
     * @param contractAddress the given contract
     * @return the contract specification
     */
    ContractSpecResponse loadContractSpecification(Address contractAddress);

}
