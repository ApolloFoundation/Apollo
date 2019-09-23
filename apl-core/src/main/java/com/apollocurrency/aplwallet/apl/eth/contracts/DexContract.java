package com.apollocurrency.aplwallet.apl.eth.contracts;

import io.reactivex.Flowable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.2.0.
 */
public class DexContract extends Contract {
    private static final String BINARY = "608060405260008060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555034801561005157600080fd5b5061172a806100616000396000f3fe6080604052600436106100705760003560e01c80637c68bebe1161004e5780637c68bebe1461029e5780638dbdbe6d14610303578063b6b55f2514610368578063ecb410541461039657610070565b80632e1a7d4d146100755780634861f39b146100b057806354ef481d14610168575b600080fd5b34801561008157600080fd5b506100ae6004803603602081101561009857600080fd5b81019080803590602001909291905050506103ff565b005b3480156100bc57600080fd5b50610109600480360360408110156100d357600080fd5b8101908080359060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610653565b60405180851515151581526020018473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018381526020018215151515815260200194505050505060405180910390f35b34801561017457600080fd5b506101b76004803603602081101561018b57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610693565b60405180806020018060200180602001848103845287818151815260200191508051906020019060200280838360005b838110156102025780820151818401526020810190506101e7565b50505050905001848103835286818151815260200191508051906020019060200280838360005b83811015610244578082015181840152602081019050610229565b50505050905001848103825285818151815260200191508051906020019060200280838360005b8381101561028657808201518184015260208101905061026b565b50505050905001965050505050505060405180910390f35b3480156102aa57600080fd5b506102ed600480360360208110156102c157600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506109ce565b6040518082815260200191505060405180910390f35b34801561030f57600080fd5b506103666004803603606081101561032657600080fd5b810190808035906020019092919080359060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610a1a565b005b6103946004803603602081101561037e57600080fd5b8101908080359060200190929190505050610c19565b005b3480156103a257600080fd5b506103e5600480360360208110156103b957600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610d67565b604051808215151515815260200191505060405180910390f35b6001151561040c33610d79565b151514610481576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260208152602001807f77697468647261773a207468652075736572206973206e6f742061637469766581525060200191505060405180910390fd5b6104896115a1565b6104938233610dd2565b9050600081602001519050600082604001519050600015158360600151151514610508576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252604a815260200180611623604a913960600191505060405180910390fd5b6105128433610ee0565b506000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614156105b4573373ffffffffffffffffffffffffffffffffffffffff166108fc829081150290604051600060405180830381858888f193505050501580156105ae573d6000803e3d6000fd5b506105e0565b6105df33828473ffffffffffffffffffffffffffffffffffffffff16610f5a9092919063ffffffff16565b5b8173ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff167f44caac6b5921bbf9e5a91b5c88bae1355a24b50a810dfd697818636334049a1e8684604051808381526020018281526020019250505060405180910390a350505050565b6000806000806106616115a1565b61066b8787610dd2565b9050806000015181602001518260400151836060015194509450945094505092959194509250565b60608060606000600160008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206001015490508060405190808252806020026020018201604052801561070e5781602001602082028038833980820191505090505b509350806040519080825280602002602001820160405280156107405781602001602082028038833980820191505090505b509250806040519080825280602002602001820160405280156107725781602001602082028038833980820191505090505b509150600080905060008090505b8281116109bb576000600160008973ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206002016000838152602001908152602001600020549050600160008973ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600301600082815260200190815260200160002060000160009054906101000a900460ff16156109ad578087848151811061085557fe5b6020026020010181815250506108696115a1565b600160008a73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060030160008381526020019081526020016000206040518060a00160405290816000820160009054906101000a900460ff161515151581526020016000820160019054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001600182015481526020016002820160009054906101000a900460ff161515151581526020016003820154815250509050806040015187858151811061097a57fe5b602002602001018181525050806080015186858151811061099757fe5b6020026020010181815250508380600101945050505b508080600101915050610780565b5084848494509450945050509193909250565b6000600160008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600101549050919050565b823360001515600160008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600301600084815260200190815260200160002060000160009054906101000a900460ff16151514610adc576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260388152602001806115eb6038913960400191505060405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff161415610b7f576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601e8152602001807f6465706f7369743a20696e76616c696420746f6b656e2061646472657373000081525060200191505060405180910390fd5b6000841415610bd9576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252604e8152602001806116a8604e913960600191505060405180910390fd5b610c063330868673ffffffffffffffffffffffffffffffffffffffff1661102b909392919063ffffffff16565b610c1285338587611131565b5050505050565b803360001515600160008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600301600084815260200190815260200160002060000160009054906101000a900460ff16151514610cdb576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260388152602001806115eb6038913960400191505060405180910390fd5b6000341415610d35576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252603b81526020018061166d603b913960400191505060405180910390fd5b610d6283336000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1634611131565b505050565b6000610d7282610d79565b9050919050565b6000600160008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060000160009054906101000a900460ff169050919050565b610dda6115a1565b600160008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060030160008481526020019081526020016000206040518060a00160405290816000820160009054906101000a900460ff161515151581526020016000820160019054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001600182015481526020016002820160009054906101000a900460ff16151515158152602001600382015481525050905092915050565b600060018060008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600301600085815260200190815260200160002060020160006101000a81548160ff0219169083151502179055506001905092915050565b611026838473ffffffffffffffffffffffffffffffffffffffff1663a9059cbb905060e01b8484604051602401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200182815260200192505050604051602081830303815290604052907bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff838183161783525050505061120b565b505050565b61112b848573ffffffffffffffffffffffffffffffffffffffff166323b872dd905060e01b858585604051602401808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018281526020019350505050604051602081830303815290604052907bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff838183161783525050505061120b565b50505050565b61113a83611338565b50611147848484846113b2565b5060018060008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600101600082825401925050819055508173ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167f2389aeb1847d9139fac317075af2ac7ace8586c66ea0fa893970980ab2be43568684604051808381526020018281526020019250505060405180910390a350505050565b61122a8273ffffffffffffffffffffffffffffffffffffffff1661158e565b61123357600080fd5b600060608373ffffffffffffffffffffffffffffffffffffffff16836040518082805190602001908083835b60208310611282578051825260208201915060208101905060208303925061125f565b6001836020036101000a0380198251168184511680821785525050505050509050019150506000604051808303816000865af19150503d80600081146112e4576040519150601f19603f3d011682016040523d82523d6000602084013e6112e9565b606091505b5091509150816112f857600080fd5b6000815111156113325780806020019051602081101561131757600080fd5b810190808051906020019092919050505061133157600080fd5b5b50505050565b600080151561134683610d79565b151514156113a95760018060008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060000160006101000a81548160ff0219169083151502179055505b60019050919050565b600084600160008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206002016000600160008873ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600101548152602001908152602001600020819055506040518060a001604052806001151581526020018473ffffffffffffffffffffffffffffffffffffffff16815260200183815260200160001515815260200142815250600160008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600301600087815260200190815260200160002060008201518160000160006101000a81548160ff02191690831515021790555060208201518160000160016101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506040820151816001015560608201518160020160006101000a81548160ff0219169083151502179055506080820151816003015590505060019050949350505050565b600080823b905060008111915050919050565b6040518060a00160405280600015158152602001600073ffffffffffffffffffffffffffffffffffffffff1681526020016000815260200160001515815260200160008152509056fe6f72646572496449734e6f744465706f73697465643a207573657220616c7265616479206465706f7369742074686973206f72646572496477697468647261773a2074686973206f7264657220496420686173206265656e20616c72656164792077697468647261776e206f722077616974696e6720666f722074686520737761706465706f7369743a2075736572206e6565647320746f207472616e736665722045544820666f722063616c6c696e672074686973206d6574686f646465706f7369743a2075736572206e6565647320746f2066696c6c207472616e7366657261626c6520746f6b656e7320616d6f756e7420666f722063616c6c696e672074686973206d6574686f64a265627a7a723058203a80f5dee2a492eec31fdfcc98bfdb4a9a3c66bed79de20f1a7f79c90d64913864736f6c634300050a0032";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final String FUNC_GETDEPOSITEDORDERDETAILS = "getDepositedOrderDetails";

    public static final String FUNC_GETUSERFILLEDDEPOSITS = "getUserFilledDeposits";

    public static final String FUNC_GETUSERDEPOSITSAMOUNT = "getUserDepositsAmount";

    public static final String FUNC_DEPOSIT = "deposit";

    public static final String FUNC_DOESUSEREXIST = "doesUserExist";

    public static final Event ASSETDEPOSITED_EVENT = new Event("AssetDeposited", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event ASSETWITHDRAWAL_EVENT = new Event("AssetWithdrawal", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected DexContract(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected DexContract(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected DexContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected DexContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<TransactionReceipt> withdraw(BigInteger orderId) {
        final Function function = new Function(
                FUNC_WITHDRAW, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<Tuple4<Boolean, String, BigInteger, Boolean>> getDepositedOrderDetails(BigInteger orderId, String user) {
        final Function function = new Function(FUNC_GETDEPOSITEDORDERDETAILS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderId),
                        new org.web3j.abi.datatypes.Address(user)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }, new TypeReference<Address>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Bool>() {
                }));
        return new RemoteCall<Tuple4<Boolean, String, BigInteger, Boolean>>(
                new Callable<Tuple4<Boolean, String, BigInteger, Boolean>>() {
                    @Override
                    public Tuple4<Boolean, String, BigInteger, Boolean> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<Boolean, String, BigInteger, Boolean>(
                                (Boolean) results.get(0).getValue(),
                                (String) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (Boolean) results.get(3).getValue());
                    }
                });
    }

    public RemoteCall<Tuple3<List<BigInteger>, List<BigInteger>, List<BigInteger>>> getUserFilledDeposits(String user) {
        final Function function = new Function(FUNC_GETUSERFILLEDDEPOSITS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(user)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint256>>() {
                }, new TypeReference<DynamicArray<Uint256>>() {
                }, new TypeReference<DynamicArray<Uint256>>() {
                }));
        return new RemoteCall<Tuple3<List<BigInteger>, List<BigInteger>, List<BigInteger>>>(
                new Callable<Tuple3<List<BigInteger>, List<BigInteger>, List<BigInteger>>>() {
                    @Override
                    public Tuple3<List<BigInteger>, List<BigInteger>, List<BigInteger>> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<List<BigInteger>, List<BigInteger>, List<BigInteger>>(
                                convertToNative((List<Uint256>) results.get(0).getValue()),
                                convertToNative((List<Uint256>) results.get(1).getValue()),
                                convertToNative((List<Uint256>) results.get(2).getValue()));
                    }
                });
    }

    public RemoteCall<BigInteger> getUserDepositsAmount(String user) {
        final Function function = new Function(FUNC_GETUSERDEPOSITSAMOUNT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(user)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<TransactionReceipt> deposit(BigInteger orderId, BigInteger amount, String token) {
        final Function function = new Function(
                FUNC_DEPOSIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderId), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Address(token)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> deposit(BigInteger orderId, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_DEPOSIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteCall<Boolean> doesUserExist(String user) {
        final Function function = new Function(FUNC_DOESUSEREXIST, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(user)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public List<AssetDepositedEventResponse> getAssetDepositedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ASSETDEPOSITED_EVENT, transactionReceipt);
        ArrayList<AssetDepositedEventResponse> responses = new ArrayList<AssetDepositedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AssetDepositedEventResponse typedResponse = new AssetDepositedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.user = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.asset = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.orderId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AssetDepositedEventResponse> assetDepositedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, AssetDepositedEventResponse>() {
            @Override
            public AssetDepositedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ASSETDEPOSITED_EVENT, log);
                AssetDepositedEventResponse typedResponse = new AssetDepositedEventResponse();
                typedResponse.log = log;
                typedResponse.user = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.asset = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.orderId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<AssetDepositedEventResponse> assetDepositedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ASSETDEPOSITED_EVENT));
        return assetDepositedEventFlowable(filter);
    }

    public List<AssetWithdrawalEventResponse> getAssetWithdrawalEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ASSETWITHDRAWAL_EVENT, transactionReceipt);
        ArrayList<AssetWithdrawalEventResponse> responses = new ArrayList<AssetWithdrawalEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AssetWithdrawalEventResponse typedResponse = new AssetWithdrawalEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.user = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.asset = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.orderId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AssetWithdrawalEventResponse> assetWithdrawalEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, AssetWithdrawalEventResponse>() {
            @Override
            public AssetWithdrawalEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ASSETWITHDRAWAL_EVENT, log);
                AssetWithdrawalEventResponse typedResponse = new AssetWithdrawalEventResponse();
                typedResponse.log = log;
                typedResponse.user = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.asset = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.orderId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<AssetWithdrawalEventResponse> assetWithdrawalEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ASSETWITHDRAWAL_EVENT));
        return assetWithdrawalEventFlowable(filter);
    }

    @Deprecated
    public static DexContract load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new DexContract(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static DexContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new DexContract(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static DexContract load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new DexContract(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static DexContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new DexContract(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<DexContract> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(DexContract.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<DexContract> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(DexContract.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<DexContract> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(DexContract.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<DexContract> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(DexContract.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class AssetDepositedEventResponse {
        public Log log;

        public String user;

        public String asset;

        public BigInteger orderId;

        public BigInteger amount;
    }

    public static class AssetWithdrawalEventResponse {
        public Log log;

        public String user;

        public String asset;

        public BigInteger orderId;

        public BigInteger amount;
    }
}
