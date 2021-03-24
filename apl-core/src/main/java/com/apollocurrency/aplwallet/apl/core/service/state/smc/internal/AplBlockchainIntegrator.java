/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.blockchain.tx.SMCTransactionReceipt;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.internal.BlockchainInfo;
import com.apollocurrency.smc.data.type.Address;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Singleton
public class AplBlockchainIntegrator implements BlockchainIntegrator {

    private final ContractService contractService;
    private final ServerInfoService serverInfoService;

    @Inject
    public AplBlockchainIntegrator(ContractService contractService, ServerInfoService serverInfoService) {
        this.contractService = Objects.requireNonNull(contractService);
        this.serverInfoService = Objects.requireNonNull(serverInfoService);
    }

    @Override
    public SMCTransactionReceipt sendMessage(Address from, Address to, String data) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public SMCTransactionReceipt sendMoney(final Address from, Address to, BigInteger amount) {
        //return aplBlockchainService.sendMoney(from, to, amount, credential.getSecret());
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public Optional<String> getSerializedObject(Address address) {
        return Optional.ofNullable(contractService.loadSerializedContract(address));
    }

    @Override
    public void putSerializedObject(SmartContract contract, String serializedContract) {
        contractService.saveSerializedContract(contract, serializedContract);
    }

    @Override
    public BlockchainInfo getBlockchainInfo() {
        BlockchainStatusDto blockchainStatus = serverInfoService.getBlockchainStatus();
        BlockchainInfo blockchainInfo = BlockchainInfo.builder()
            .chainId(blockchainStatus.getChainId().toString())
            .height(blockchainStatus.getNumberOfBlocks())
            .blockId(blockchainStatus.getLastBlock())
            .timestamp(blockchainStatus.getTime())
            .build();
        return blockchainInfo;
    }
}
