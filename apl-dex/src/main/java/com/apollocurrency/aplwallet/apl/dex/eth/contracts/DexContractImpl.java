package com.apollocurrency.aplwallet.apl.dex.eth.contracts;

import com.apollocurrency.aplwallet.apl.dex.eth.service.EthereumWalletService;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

public class DexContractImpl extends DexContract {

    public DexContractImpl(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(contractAddress, web3j, credentials, contractGasProvider);
    }

    public DexContractImpl(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, EthereumWalletService ethereumWalletService) {
        super(contractAddress, web3j, transactionManager, contractGasProvider, ethereumWalletService);
    }
}
