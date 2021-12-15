/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.smc.model.AplContractSpec;
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
     * Generate the uniq smart-contract address.
     *
     * @param account the transaction sender account id
     * @param salt    salt for generation routine
     * @return generated public key for the published smart-contract
     */
    byte[] generatePublicKey(long account, String salt);

}
