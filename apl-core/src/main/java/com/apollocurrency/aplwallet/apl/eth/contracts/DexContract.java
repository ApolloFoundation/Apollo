package com.apollocurrency.aplwallet.apl.eth.contracts;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple8;
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
public class DexContract extends Contract {
    private static final String BINARY = "600080546001600160a01b0319908116825560c06040819052610258608081905262ed4e0060a0819052600491909155600555600280549092163317918290556001600160a01b039190911691907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908290a3611be8806100816000396000f3fe60806040526004361061011f5760003560e01c80638da5cb5b116100a0578063b6b55f2511610064578063b6b55f251461047a578063ecb4105414610497578063eda1122c146104ca578063f2fde38b146104f4578063f7b3ae2f146105275761011f565b80638da5cb5b146103795780638dbdbe6d146103aa5780638f32d59b146103e95780639fe21fc414610412578063a4cdbd141461044a5761011f565b8063715018a6116100e7578063715018a61461026957806371eedb881461027e57806377b0dd70146102ae5780637c68bebe146103015780637dc4da2e146103345761011f565b806318df0038146101245780632e1a7d4d146101725780634861f39b1461019e578063571694cd146101ff5780636d72dc381461023b575b600080fd5b34801561013057600080fd5b5061014e6004803603602081101561014757600080fd5b50356105ca565b6040518082600181111561015e57fe5b60ff16815260200191505060405180910390f35b34801561017e57600080fd5b5061019c6004803603602081101561019557600080fd5b50356105dd565b005b3480156101aa57600080fd5b506101d7600480360360408110156101c157600080fd5b50803590602001356001600160a01b0316610754565b6040805193151584526001600160a01b03909216602084015282820152519081900360600190f35b34801561020b57600080fd5b506102296004803603602081101561022257600080fd5b5035610786565b60408051918252519081900360200190f35b34801561024757600080fd5b50610250610791565b6040805192835260208301919091528051918290030190f35b34801561027557600080fd5b5061019c61079b565b34801561028a57600080fd5b5061019c600480360360408110156102a157600080fd5b50803590602001356107f6565b3480156102ba57600080fd5b5061019c600480360360c08110156102d157600080fd5b508035906020810135906001600160a01b0360408201358116916060810135916080820135169060a00135610991565b34801561030d57600080fd5b506102296004803603602081101561032457600080fd5b50356001600160a01b03166109b0565b34801561034057600080fd5b5061019c6004803603608081101561035757600080fd5b508035906020810135906001600160a01b0360408201351690606001356109cf565b34801561038557600080fd5b5061038e610bc2565b604080516001600160a01b039092168252519081900360200190f35b3480156103b657600080fd5b5061019c600480360360608110156103cd57600080fd5b50803590602081013590604001356001600160a01b0316610bd1565b3480156103f557600080fd5b506103fe610cf6565b604080519115158252519081900360200190f35b61019c6004803603608081101561042857600080fd5b508035906020810135906001600160a01b036040820135169060600135610d07565b34801561045657600080fd5b5061019c6004803603604081101561046d57600080fd5b5080359060200135610d22565b61019c6004803603602081101561049057600080fd5b5035610dd4565b3480156104a357600080fd5b506103fe600480360360208110156104ba57600080fd5b50356001600160a01b0316610e8f565b3480156104d657600080fd5b5061019c600480360360208110156104ed57600080fd5b5035610e9a565b34801561050057600080fd5b5061019c6004803603602081101561051757600080fd5b50356001600160a01b031661109d565b34801561053357600080fd5b506105516004803603602081101561054a57600080fd5b50356110ba565b60405180898152602001888152602001878152602001868152602001856001600160a01b03166001600160a01b03168152602001846001600160a01b03166001600160a01b031681526020018381526020018260038111156105af57fe5b60ff1681526020019850505050505050505060405180910390f35b60006105d5826111b5565b90505b919050565b6105e6336111ec565b151560011461063c576040805162461bcd60e51b815260206004820181905260248201527f77697468647261773a207468652075736572206973206e6f7420616374697665604482015290519081900360640190fd5b61064461171c565b61064e823361120a565b6020810151604082015191925090806106985760405162461bcd60e51b8152600401808060200182810382526043815260200180611a2a6043913960600191505060405180910390fd5b6106a28433611268565b506000546001600160a01b03838116911614156106ec57604051339082156108fc029083906000818181858888f193505050501580156106e6573d6000803e3d6000fd5b50610706565b6107066001600160a01b038316338363ffffffff61129516565b604080518581526020810183905281516001600160a01b0385169233927f44caac6b5921bbf9e5a91b5c88bae1355a24b50a810dfd697818636334049a1e929081900390910190a350505050565b600080600061076161171c565b61076b868661120a565b80516020820151604090920151909891975095509350505050565b60006105d5826112e7565b6004546005549091565b6107a3610cf6565b6107ac57600080fd5b6002546040516000916001600160a01b0316907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908390a3600280546001600160a01b0319169055565b8033600160008381526003602081905260409091206008015460ff169081111561081c57fe5b146108585760405162461bcd60e51b815260040180806020018281038252604f81526020018061195f604f913960600191505060405180910390fd5b6000828152600360205260409020600401546001600160a01b038281169116146108b35760405162461bcd60e51b81526004018080602001828103825260418152602001806118526041913960600191505060405180910390fd5b600082815260036020526040902060010154428111156109045760405162461bcd60e51b815260040180806020018281038252602d8152602001806119ae602d913960400191505060405180910390fd5b600084815260036020819052604090912060088101805460ff1916909217909155600681015460079091015461094791879133916001600160a01b031690611396565b5060408051868152602081018690524281830152905133917f222b84eaf6103dc4589b81de12d7b2f6a1d0b45039451d2af236473910c4a240919081900360600190a25050505050565b61099c868686610bd1565b6109a8868484846109cf565b505050505050565b6001600160a01b03166000908152600160208190526040909120015490565b826000808281526003602081905260409091206008015460ff16908111156109f357fe5b14610a2f5760405162461bcd60e51b8152600401808060200182810382526049815260200180611b6b6049913960600191505060405180910390fd5b610a3b82603c02611427565b610a4361171c565b610a4d863361120a565b80519091501515600114610a925760405162461bcd60e51b815260040180806020018281038252603e815260200180611a6d603e913960400191505060405180910390fd5b6040810151610ad25760405162461bcd60e51b815260040180806020018281038252604381526020018061180f6043913960600191505060405180910390fd5b610adc8633611268565b50602081810180516000888152600384526040908190206006810180546001600160a01b03199081166001600160a01b0395861617909155828701516007830181905560088301805460ff191660019081179091556004840180548416339081179091556005850180548e8916951685179055600285018e905542808655603c8d0281019290950191909155955184518e81529788018d905287850193909352606087018a9052608087015291519216939092917f91acd19eaf946e9dde3bbe5c8b1d0c4e95684f4bbce98fcd2c53e8777af7c6369181900360a00190a4505050505050565b6002546001600160a01b031690565b33600081815260016020908152604080832087845260030190915290205484919060ff1615610c315760405162461bcd60e51b81526004018080602001828103825260388152602001806117876038913960400191505060405180910390fd5b6001600160a01b038316610c8c576040805162461bcd60e51b815260206004820152601e60248201527f6465706f7369743a20696e76616c696420746f6b656e20616464726573730000604482015290519081900360640190fd5b83610cc85760405162461bcd60e51b815260040180806020018281038252604e815260200180611911604e913960600191505060405180910390fd5b610ce36001600160a01b03841633308763ffffffff6114a916565b610cef85338587611503565b5050505050565b6002546001600160a01b0316331490565b610d1084610dd4565b610d1c848484846109cf565b50505050565b610d2a610cf6565b610d3357600080fd5b81610d6f5760405162461bcd60e51b81526004018080602001828103825260438152602001806118ce6043913960600191505060405180910390fd5b81811015610dae5760405162461bcd60e51b8152600401808060200182810382526041815260200180611aab6041913960600191505060405180910390fd5b60408051808201909152603c928302808252919092026020909201829052600455600555565b33600081815260016020908152604080832085845260030190915290205482919060ff1615610e345760405162461bcd60e51b81526004018080602001828103825260388152602001806117876038913960400191505060405180910390fd5b34610e705760405162461bcd60e51b815260040180806020018281038252603b815260200180611893603b913960400191505060405180910390fd5b600054610e8a90849033906001600160a01b031634611503565b505050565b60006105d5826111ec565b806000610ea6826112e7565b9050600160008281526003602081905260409091206008015460ff1690811115610ecc57fe5b14610f085760405162461bcd60e51b815260040180806020018281038252604f815260200180611b1c604f913960600191505060405180910390fd5b600081815260036020526040902060010154428111610f585760405162461bcd60e51b8152600401808060200182810382526030815260200180611aec6030913960400191505060405180910390fd5b6000610f63856112e7565b600081815260036020526040812060088101805460ff19166002179055600501549192506001600160a01b0390911690610f9c836111b5565b6001811115610fa757fe5b1415610ffc5760008281526003602052604080822060070154905183926001600160a01b0384169280156108fc02929091818181858888f19350505050158015610ff5573d6000803e3d6000fd5b5050611032565b60008281526003602052604090206007810154600690910154611032916001600160a01b0390911690839063ffffffff61129516565b6000828152600360208181526040928390209091018890558151848152429181019190915280820188905290516001600160a01b038316917fd8c410238ddcadd482d857c422e62972bcd5b63864095af0499a9c530d777c8d919081900360600190a2505050505050565b6110a5610cf6565b6110ae57600080fd5b6110b781611573565b50565b6000806000806000806000806110ce61173c565b60008a8152600360208181526040928390208351610120810185528154815260018201549281019290925260028101549382019390935282820154606082015260048301546001600160a01b0390811660808301526005840154811660a083015260068401541660c0820152600783015460e0820152600883015490929161010084019160ff169081111561115f57fe5b600381111561116a57fe5b815250509050806000015181602001518260400151836060015184608001518560c001518660e001518761010001519850985098509850985098509850985050919395975091939597565b60008054828252600360205260408220600601546001600160a01b03908116911614156111e4575060006105d8565b5060016105d8565b6001600160a01b031660009081526001602052604090205460ff1690565b61121261171c565b506001600160a01b0380821660009081526001602081815260408084208785526003018252928390208351606081018552815460ff81161515825261010090049095169185019190915201549082015292915050565b6001600160a01b031660009081526001602081815260408084209484526003909401905291812082015590565b604080516001600160a01b038416602482015260448082018490528251808303909101815260649091019091526020810180516001600160e01b031663a9059cbb60e01b179052610e8a9084906115e2565b6000600282604051602001808281526020019150506040516020818303038152906040526040518082805190602001908083835b6020831061133a5780518252601f19909201916020918201910161131b565b51815160209384036101000a60001901801990921691161790526040519190930194509192505080830381855afa158015611379573d6000803e3d6000fd5b5050506040513d602081101561138e57600080fd5b505192915050565b6001600160a01b039283166000908152600160208181526040808420808401805486526002820184528286208a90558251606081018452858152978916888501908152888401978852998652600390910190925290922093518454965160ff1990971690151517610100600160a81b0319166101009690951695909502939093178255519083015580548201905590565b6004548110156114685760405162461bcd60e51b815260040180806020018281038252604f8152602001806119db604f913960600191505060405180910390fd5b6005548111156110b75760405162461bcd60e51b81526004018080602001828103825260508152602001806117bf6050913960600191505060405180910390fd5b604080516001600160a01b0385811660248301528416604482015260648082018490528251808303909101815260849091019091526020810180516001600160e01b03166323b872dd60e01b179052610d1c9085906115e2565b61150c836116d8565b5061151984848484611396565b50816001600160a01b0316836001600160a01b03167f2389aeb1847d9139fac317075af2ac7ace8586c66ea0fa893970980ab2be43568684604051808381526020018281526020019250505060405180910390a350505050565b6001600160a01b03811661158657600080fd5b6002546040516001600160a01b038084169216907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e090600090a3600280546001600160a01b0319166001600160a01b0392909216919091179055565b6115f4826001600160a01b0316611716565b6115fd57600080fd5b60006060836001600160a01b0316836040518082805190602001908083835b6020831061163b5780518252601f19909201916020918201910161161c565b6001836020036101000a0380198251168184511680821785525050505050509050019150506000604051808303816000865af19150503d806000811461169d576040519150601f19603f3d011682016040523d82523d6000602084013e6116a2565b606091505b5091509150816116b157600080fd5b805115610d1c578080602001905160208110156116cd57600080fd5b5051610d1c57600080fd5b60006116e3826111ec565b61170e576001600160a01b0382166000908152600160208190526040909120805460ff191690911790555b506001919050565b3b151590565b604080516060810182526000808252602082018190529181019190915290565b6040805161012081018252600080825260208201819052918101829052606081018290526080810182905260a0810182905260c0810182905260e08101829052906101008201529056fe6f72646572496449734e6f744465706f73697465643a207573657220616c7265616479206465706f7369742074686973206f7264657249645f76616c6964617465526566756e6454696d657374616d703a207468652074696d657374616d702073686f756c6420626520736d616c6c6572207468616e206d61782073776170206c69666574696d65696e6974696174653a2074686973206f7264657220496420686173206265656e2066696e6973686564206f722077616974696e6720666f72207468652072656465656d6973526566756e6461626c653a206f6e6c792074686520696e69746961746f72206f662074686520737761702063616e2063616c6c2074686973206d6574686f646465706f7369743a2075736572206e6565647320746f207472616e736665722045544820666f722063616c6c696e672074686973206d6574686f646368616e6765537761704c69666574696d654c696d6974733a206e65774d696e20616e64206e65774d61782073686f756c6420626520626967676572207468656e20306465706f7369743a2075736572206e6565647320746f2066696c6c207472616e7366657261626c6520746f6b656e7320616d6f756e7420666f722063616c6c696e672074686973206d6574686f646973526566756e6461626c653a20746865207377617020776974682074686973207365637265744861736820646f6573206e6f74206578697374206f7220686173206265656e2066696e69736865646973526566756e6461626c653a2074686520726566756e64206973206e6f7420617661696c61626c65206e6f775f76616c6964617465526566756e6454696d657374616d703a207468652074696d657374616d702073686f756c6420626520626967676572207468616e206d696e2073776170206c69666574696d6577697468647261773a2074686973206f7264657220496420686173206265656e2066696e6973686564206f722077616974696e6720666f72207468652072656465656d696e6974696174653a2074686973206f7264657220496420686173206e6f74206265656e206372656174656420616e64206465706f7369746564207965746368616e6765537761704c69666574696d654c696d6974733a20746865206e65774d61782073686f756c6420626520626967676572207468656e206e65774d6178697352656465656d61626c653a207468652072656465656d20697320636c6f73656420666f7220746869732073776170697352656465656d61626c653a20746865207377617020776974682074686973207365637265744861736820646f6573206e6f74206578697374206f7220686173206265656e2066696e697368656469734e6f74496e697469617465643a20746869732073656372657420686173682077617320616c726561647920757365642c20706c656173652075736520616e6f74686572206f6e65a265627a7a7230582018dec9904070226340770f7c0e49ac49a279c39f90d7082861d8048215caea3e64736f6c634300050a0032";

    public static final String FUNC_GETSWAPTYPE = "getSwapType";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final String FUNC_GETDEPOSITEDORDERDETAILS = "getDepositedOrderDetails";

    public static final String FUNC_GETHASHOFSECRET = "getHashOfSecret";

    public static final String FUNC_GETSWAPLIFETIMELIMITS = "getSwapLifetimeLimits";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_REFUND = "refund";

    public static final String FUNC_DEPOSITANDINITIATE = "depositAndInitiate";

    public static final String FUNC_GETUSERDEPOSITSAMOUNT = "getUserDepositsAmount";

    public static final String FUNC_INITIATE = "initiate";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_DEPOSIT = "deposit";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_CHANGESWAPLIFETIMELIMITS = "changeSwapLifetimeLimits";

    public static final String FUNC_DOESUSEREXIST = "doesUserExist";

    public static final String FUNC_REDEEM = "redeem";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_GETSWAPDATA = "getSwapData";

    public static final Event INITIATED_EVENT = new Event("Initiated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event REDEEMED_EVENT = new Event("Redeemed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event REFUNDED_EVENT = new Event("Refunded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

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

    public RemoteCall<BigInteger> getSwapType(byte[] secretHash) {
        final Function function = new Function(FUNC_GETSWAPTYPE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(secretHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<TransactionReceipt> withdraw(BigInteger orderId) {
        final Function function = new Function(
                FUNC_WITHDRAW, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<Tuple3<Boolean, String, BigInteger>> getDepositedOrderDetails(BigInteger orderId, String user) {
        final Function function = new Function(FUNC_GETDEPOSITEDORDERDETAILS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderId), 
                new org.web3j.abi.datatypes.Address(user)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}, new TypeReference<Address>() {}, new TypeReference<Uint256>() {}));
        return new RemoteCall<Tuple3<Boolean, String, BigInteger>>(
                new Callable<Tuple3<Boolean, String, BigInteger>>() {
                    @Override
                    public Tuple3<Boolean, String, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<Boolean, String, BigInteger>(
                                (Boolean) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue());
                    }
                });
    }

    public RemoteCall<byte[]> getHashOfSecret(byte[] secret) {
        final Function function = new Function(FUNC_GETHASHOFSECRET, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(secret)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteCall<Tuple2<BigInteger, BigInteger>> getSwapLifetimeLimits() {
        final Function function = new Function(FUNC_GETSWAPLIFETIMELIMITS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteCall<Tuple2<BigInteger, BigInteger>>(
                new Callable<Tuple2<BigInteger, BigInteger>>() {
                    @Override
                    public Tuple2<BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue());
                    }
                });
    }

    public RemoteCall<TransactionReceipt> renounceOwnership() {
        final Function function = new Function(
                FUNC_RENOUNCEOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> refund(BigInteger orderId, byte[] secretHash) {
        final Function function = new Function(
                FUNC_REFUND, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderId), 
                new org.web3j.abi.datatypes.generated.Bytes32(secretHash)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> depositAndInitiate(BigInteger orderId, BigInteger amount, String token, byte[] secretHash, String recipient, BigInteger refundTimestamp) {
        final Function function = new Function(
                FUNC_DEPOSITANDINITIATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderId), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Address(token), 
                new org.web3j.abi.datatypes.generated.Bytes32(secretHash), 
                new org.web3j.abi.datatypes.Address(recipient), 
                new org.web3j.abi.datatypes.generated.Uint256(refundTimestamp)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<BigInteger> getUserDepositsAmount(String user) {
        final Function function = new Function(FUNC_GETUSERDEPOSITSAMOUNT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(user)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<TransactionReceipt> initiate(BigInteger orderId, byte[] secretHash, String recipient, BigInteger refundTimestamp) {
        final Function function = new Function(
                FUNC_INITIATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderId), 
                new org.web3j.abi.datatypes.generated.Bytes32(secretHash), 
                new org.web3j.abi.datatypes.Address(recipient), 
                new org.web3j.abi.datatypes.generated.Uint256(refundTimestamp)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
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

    public RemoteCall<Boolean> isOwner() {
        final Function function = new Function(FUNC_ISOWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<TransactionReceipt> depositAndInitiate(BigInteger orderId, byte[] secretHash, String recipient, BigInteger refundTimestamp, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_DEPOSITANDINITIATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderId), 
                new org.web3j.abi.datatypes.generated.Bytes32(secretHash), 
                new org.web3j.abi.datatypes.Address(recipient), 
                new org.web3j.abi.datatypes.generated.Uint256(refundTimestamp)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteCall<TransactionReceipt> changeSwapLifetimeLimits(BigInteger newMin, BigInteger newMax) {
        final Function function = new Function(
                FUNC_CHANGESWAPLIFETIMELIMITS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(newMin), 
                new org.web3j.abi.datatypes.generated.Uint256(newMax)), 
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

    public RemoteCall<TransactionReceipt> redeem(byte[] secret) {
        final Function function = new Function(
                FUNC_REDEEM, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(secret)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> transferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<Tuple8<BigInteger, BigInteger, byte[], byte[], String, String, BigInteger, BigInteger>> getSwapData(byte[] secretHash) {
        final Function function = new Function(FUNC_GETSWAPDATA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(secretHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Address>() {}, new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint8>() {}));
        return new RemoteCall<Tuple8<BigInteger, BigInteger, byte[], byte[], String, String, BigInteger, BigInteger>>(
                new Callable<Tuple8<BigInteger, BigInteger, byte[], byte[], String, String, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple8<BigInteger, BigInteger, byte[], byte[], String, String, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple8<BigInteger, BigInteger, byte[], byte[], String, String, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (byte[]) results.get(2).getValue(), 
                                (byte[]) results.get(3).getValue(), 
                                (String) results.get(4).getValue(), 
                                (String) results.get(5).getValue(), 
                                (BigInteger) results.get(6).getValue(), 
                                (BigInteger) results.get(7).getValue());
                    }
                });
    }

    public List<InitiatedEventResponse> getInitiatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(INITIATED_EVENT, transactionReceipt);
        ArrayList<InitiatedEventResponse> responses = new ArrayList<InitiatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            InitiatedEventResponse typedResponse = new InitiatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.initiator = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.recipient = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.asset = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.orderId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.secretHash = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.initTimestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.refundTimestamp = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<InitiatedEventResponse> initiatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, InitiatedEventResponse>() {
            @Override
            public InitiatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(INITIATED_EVENT, log);
                InitiatedEventResponse typedResponse = new InitiatedEventResponse();
                typedResponse.log = log;
                typedResponse.initiator = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.recipient = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.asset = (String) eventValues.getIndexedValues().get(2).getValue();
                typedResponse.orderId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.secretHash = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.initTimestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.refundTimestamp = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
                typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<InitiatedEventResponse> initiatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(INITIATED_EVENT));
        return initiatedEventFlowable(filter);
    }

    public List<RedeemedEventResponse> getRedeemedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(REDEEMED_EVENT, transactionReceipt);
        ArrayList<RedeemedEventResponse> responses = new ArrayList<RedeemedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RedeemedEventResponse typedResponse = new RedeemedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.redeemer = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.secretHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.redeemTimestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.secret = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<RedeemedEventResponse> redeemedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, RedeemedEventResponse>() {
            @Override
            public RedeemedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(REDEEMED_EVENT, log);
                RedeemedEventResponse typedResponse = new RedeemedEventResponse();
                typedResponse.log = log;
                typedResponse.redeemer = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.secretHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.redeemTimestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.secret = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<RedeemedEventResponse> redeemedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REDEEMED_EVENT));
        return redeemedEventFlowable(filter);
    }

    public List<RefundedEventResponse> getRefundedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(REFUNDED_EVENT, transactionReceipt);
        ArrayList<RefundedEventResponse> responses = new ArrayList<RefundedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RefundedEventResponse typedResponse = new RefundedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.refunder = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.orderId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.secretHash = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.refundTime = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<RefundedEventResponse> refundedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, RefundedEventResponse>() {
            @Override
            public RefundedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(REFUNDED_EVENT, log);
                RefundedEventResponse typedResponse = new RefundedEventResponse();
                typedResponse.log = log;
                typedResponse.refunder = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.orderId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.secretHash = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.refundTime = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<RefundedEventResponse> refundedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REFUNDED_EVENT));
        return refundedEventFlowable(filter);
    }

    public List<OwnershipTransferredEventResponse> getOwnershipTransferredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<OwnershipTransferredEventResponse> responses = new ArrayList<OwnershipTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, OwnershipTransferredEventResponse>() {
            @Override
            public OwnershipTransferredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, log);
                OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
                typedResponse.log = log;
                typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERSHIPTRANSFERRED_EVENT));
        return ownershipTransferredEventFlowable(filter);
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

    public static class InitiatedEventResponse {
        public Log log;

        public String initiator;

        public String recipient;

        public String asset;

        public BigInteger orderId;

        public byte[] secretHash;

        public BigInteger initTimestamp;

        public BigInteger refundTimestamp;

        public BigInteger amount;
    }

    public static class RedeemedEventResponse {
        public Log log;

        public String redeemer;

        public byte[] secretHash;

        public BigInteger redeemTimestamp;

        public byte[] secret;
    }

    public static class RefundedEventResponse {
        public Log log;

        public String refunder;

        public BigInteger orderId;

        public byte[] secretHash;

        public BigInteger refundTime;
    }

    public static class OwnershipTransferredEventResponse {
        public Log log;

        public String previousOwner;

        public String newOwner;
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
