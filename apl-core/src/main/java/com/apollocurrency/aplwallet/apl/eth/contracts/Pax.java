package com.apollocurrency.aplwallet.apl.eth.contracts;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.2.0.
 */
public class Pax extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b5060405160208061080d83398101604081815291517f6f72672e7a657070656c696e6f732e70726f78792e696d706c656d656e74617482527f696f6e00000000000000000000000000000000000000000000000000000000006020830152915190819003602301902081906000805160206107ed8339815191521461009157fe5b6100a381640100000000610104810204565b50604080517f6f72672e7a657070656c696e6f732e70726f78792e61646d696e0000000000008152905190819003601a0190206000805160206107cd833981519152146100ec57fe5b6100fe336401000000006101c2810204565b506101dc565b600061011c826401000000006105ae6101d482021704565b15156101af57604080517f08c379a000000000000000000000000000000000000000000000000000000000815260206004820152603b60248201527f43616e6e6f742073657420612070726f787920696d706c656d656e746174696f60448201527f6e20746f2061206e6f6e2d636f6e747261637420616464726573730000000000606482015290519081900360840190fd5b506000805160206107ed83398151915255565b6000805160206107cd83398151915255565b6000903b1190565b6105e2806101eb6000396000f30060806040526004361061006c5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633659cfe681146100765780634f1ef286146100975780635c60da1b146100b75780638f283970146100e8578063f851a44014610109575b61007461011e565b005b34801561008257600080fd5b50610074600160a060020a0360043516610138565b61007460048035600160a060020a03169060248035908101910135610172565b3480156100c357600080fd5b506100cc6101ea565b60408051600160a060020a039092168252519081900360200190f35b3480156100f457600080fd5b50610074600160a060020a0360043516610227565b34801561011557600080fd5b506100cc610339565b610126610364565b610136610131610411565b610436565b565b61014061045a565b600160a060020a031633600160a060020a03161415610167576101628161047f565b61016f565b61016f61011e565b50565b61017a61045a565b600160a060020a031633600160a060020a031614156101dd5761019c8361047f565b30600160a060020a03163483836040518083838082843782019150509250505060006040518083038185875af19250505015156101d857600080fd5b6101e5565b6101e561011e565b505050565b60006101f461045a565b600160a060020a031633600160a060020a0316141561021c57610215610411565b9050610224565b61022461011e565b90565b61022f61045a565b600160a060020a031633600160a060020a0316141561016757600160a060020a03811615156102e557604080517f08c379a000000000000000000000000000000000000000000000000000000000815260206004820152603660248201527f43616e6e6f74206368616e6765207468652061646d696e206f6620612070726f60448201527f787920746f20746865207a65726f206164647265737300000000000000000000606482015290519081900360840190fd5b7f7e644d79422f17c01e4894b5f4f588d331ebfa28653d42ae832dc59e38c9798f61030e61045a565b60408051600160a060020a03928316815291841660208301528051918290030190a1610162816104c7565b600061034361045a565b600160a060020a031633600160a060020a0316141561021c5761021561045a565b61036c61045a565b600160a060020a031633141561040957604080517f08c379a000000000000000000000000000000000000000000000000000000000815260206004820152603260248201527f43616e6e6f742063616c6c2066616c6c6261636b2066756e6374696f6e20667260448201527f6f6d207468652070726f78792061646d696e0000000000000000000000000000606482015290519081900360840190fd5b610136610136565b7f7050c9e0f4ca769c69bd3a8ef740bc37934f8e2c036e5a723fd8ee048ed3f8c35490565b3660008037600080366000845af43d6000803e808015610455573d6000f35b3d6000fd5b7f10d6a54a4754c8869d6886b5f5d7fbfa5b4522237ea5c60d11bc4e7a1ff9390b5490565b610488816104eb565b60408051600160a060020a038316815290517fbc7cd75a20ee27fd9adebab32041f755214dbc6bffa90cc0225b39da2e5c2d3b9181900360200190a150565b7f10d6a54a4754c8869d6886b5f5d7fbfa5b4522237ea5c60d11bc4e7a1ff9390b55565b60006104f6826105ae565b151561058957604080517f08c379a000000000000000000000000000000000000000000000000000000000815260206004820152603b60248201527f43616e6e6f742073657420612070726f787920696d706c656d656e746174696f60448201527f6e20746f2061206e6f6e2d636f6e747261637420616464726573730000000000606482015290519081900360840190fd5b507f7050c9e0f4ca769c69bd3a8ef740bc37934f8e2c036e5a723fd8ee048ed3f8c355565b6000903b11905600a165627a7a72305820e6a0e94ea36527366a6a00202c99a28a36e0c44d2d0594a734640045e7169670002910d6a54a4754c8869d6886b5f5d7fbfa5b4522237ea5c60d11bc4e7a1ff9390b7050c9e0f4ca769c69bd3a8ef740bc37934f8e2c036e5a723fd8ee048ed3f8c30000000000000000000000006ffcb0f00c3ad2575e443152d8861aec1bda9ce6";

    public static final String FUNC_UPGRADETO = "upgradeTo";

    public static final String FUNC_UPGRADETOANDCALL = "upgradeToAndCall";

    public static final String FUNC_IMPLEMENTATION = "implementation";

    public static final String FUNC_CHANGEADMIN = "changeAdmin";

    public static final String FUNC_ADMIN = "admin";

    public static final Event ADMINCHANGED_EVENT = new Event("AdminChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event UPGRADED_EVENT = new Event("Upgraded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    @Deprecated
    protected Pax(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Pax(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Pax(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Pax(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<TransactionReceipt> upgradeTo(String newImplementation) {
        final Function function = new Function(
                FUNC_UPGRADETO, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newImplementation)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> upgradeToAndCall(String newImplementation, byte[] data, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_UPGRADETOANDCALL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newImplementation), 
                new org.web3j.abi.datatypes.DynamicBytes(data)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteCall<String> implementation() {
        final Function function = new Function(FUNC_IMPLEMENTATION, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<TransactionReceipt> changeAdmin(String newAdmin) {
        final Function function = new Function(
                FUNC_CHANGEADMIN, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newAdmin)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<String> admin() {
        final Function function = new Function(FUNC_ADMIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public List<AdminChangedEventResponse> getAdminChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ADMINCHANGED_EVENT, transactionReceipt);
        ArrayList<AdminChangedEventResponse> responses = new ArrayList<AdminChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AdminChangedEventResponse typedResponse = new AdminChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.previousAdmin = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newAdmin = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AdminChangedEventResponse> adminChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, AdminChangedEventResponse>() {
            @Override
            public AdminChangedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADMINCHANGED_EVENT, log);
                AdminChangedEventResponse typedResponse = new AdminChangedEventResponse();
                typedResponse.log = log;
                typedResponse.previousAdmin = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.newAdmin = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<AdminChangedEventResponse> adminChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADMINCHANGED_EVENT));
        return adminChangedEventFlowable(filter);
    }

    public List<UpgradedEventResponse> getUpgradedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(UPGRADED_EVENT, transactionReceipt);
        ArrayList<UpgradedEventResponse> responses = new ArrayList<UpgradedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UpgradedEventResponse typedResponse = new UpgradedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.implementation = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<UpgradedEventResponse> upgradedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, UpgradedEventResponse>() {
            @Override
            public UpgradedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(UPGRADED_EVENT, log);
                UpgradedEventResponse typedResponse = new UpgradedEventResponse();
                typedResponse.log = log;
                typedResponse.implementation = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<UpgradedEventResponse> upgradedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(UPGRADED_EVENT));
        return upgradedEventFlowable(filter);
    }

    @Deprecated
    public static Pax load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Pax(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Pax load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Pax(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Pax load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Pax(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Pax load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Pax(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<Pax> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String _implementation) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_implementation)));
        return deployRemoteCall(Pax.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<Pax> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String _implementation) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_implementation)));
        return deployRemoteCall(Pax.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Pax> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String _implementation) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_implementation)));
        return deployRemoteCall(Pax.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Pax> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String _implementation) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_implementation)));
        return deployRemoteCall(Pax.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class AdminChangedEventResponse {
        public Log log;

        public String previousAdmin;

        public String newAdmin;
    }

    public static class UpgradedEventResponse {
        public Log log;

        public String implementation;
    }
}
