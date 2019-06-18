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
    private static final String BINARY = "600080546001600160a01b0319908116825560c06040819052610258608081905262ed4e0060a0819052600491909155600555600280549092163317918290556001600160a01b039190911691907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908290a3611a3a806100816000396000f3fe6080604052600436106101095760003560e01c80638da5cb5b11610095578063b31597ad11610064578063b31597ad146103ad578063ccc41f24146103dd578063ecb41054146103e5578063f2fde38b14610418578063f7b3ae2f1461044b57610109565b80638da5cb5b146102ea5780638f32d59b1461031b57806395e213d214610344578063a4cdbd141461037d57610109565b806361e3c523116100dc57806361e3c523146102185780636d72dc3814610253578063715018a6146102815780637249fbb6146102965780638d92fdf3146102c057610109565b806318df00381461010e5780634468451b1461015c57806348e558da14610197578063571694cd146101dc575b600080fd5b34801561011a57600080fd5b506101386004803603602081101561013157600080fd5b50356104ee565b6040518082600181111561014857fe5b60ff16815260200191505060405180910390f35b34801561016857600080fd5b506101956004803603604081101561017f57600080fd5b50803590602001356001600160a01b0316610526565b005b3480156101a357600080fd5b50610195600480360360808110156101ba57600080fd5b508035906001600160a01b03602082013516906040810135906060013561062d565b3480156101e857600080fd5b50610206600480360360208110156101ff57600080fd5b50356107b3565b60408051918252519081900360200190f35b34801561022457600080fd5b506102066004803603604081101561023b57600080fd5b506001600160a01b0381358116916020013516610862565b34801561025f57600080fd5b50610268610875565b6040805192835260208301919091528051918290030190f35b34801561028d57600080fd5b5061019561087f565b3480156102a257600080fd5b50610195600480360360208110156102b957600080fd5b50356108da565b3480156102cc57600080fd5b50610195600480360360208110156102e357600080fd5b5035610a83565b3480156102f657600080fd5b506102ff610b76565b604080516001600160a01b039092168252519081900360200190f35b34801561032757600080fd5b50610330610b85565b604080519115158252519081900360200190f35b34801561035057600080fd5b506101956004803603604081101561036757600080fd5b50803590602001356001600160a01b0316610b96565b34801561038957600080fd5b50610195600480360360408110156103a057600080fd5b5080359060200135610c92565b3480156103b957600080fd5b50610195600480360360408110156103d057600080fd5b5080359060200135610d4a565b610195610fbd565b3480156103f157600080fd5b506103306004803603602081101561040857600080fd5b50356001600160a01b0316611066565b34801561042457600080fd5b506101956004803603602081101561043b57600080fd5b50356001600160a01b0316611077565b34801561045757600080fd5b506104756004803603602081101561046e57600080fd5b5035611094565b60405180898152602001888152602001878152602001868152602001856001600160a01b03166001600160a01b03168152602001846001600160a01b03166001600160a01b031681526020018381526020018260038111156104d357fe5b60ff1681526020019850505050505050505060405180910390f35b60008054828252600360205260408220600501546001600160a01b039081169116141561051d57506000610521565b5060015b919050565b61052f33611181565b151560011461057257604051600160e51b62461bcd0281526004018080602001828103825260258152602001806116216025913960400191505060405180910390fd5b600061057e338361119f565b9050828110156105c257604051600160e51b62461bcd02815260040180806020018281038252602381526020018061167b6023913960400191505060405180910390fd5b6105cd3383856111cb565b506105e86001600160a01b038316338563ffffffff61123c16565b6040805184815290516001600160a01b0384169133917fe67e5b35cdbd93a082969ccad448ff849669747b73ebe813df7a5d1100b1982a9181900360200190a3505050565b836000808281526003602081905260409091206007015460ff169081111561065157fe5b1461069057604051600160e51b62461bcd0281526004018080602001828103825260498152602001806119c66049913960600191505060405180910390fd5b826106cf57604051600160e51b62461bcd0281526004018080602001828103825260358152602001806116466035913960400191505060405180910390fd5b6106d882611296565b6106e3338585611327565b6106ee3385856111cb565b506000858152600360209081526040918290206005810180546001600160a01b038981166001600160a01b031992831617928390556006840189905560078401805460ff1916600190811790915560048501805433941684179055600285018c905542808655603c8a028101919095015585518b815294850193909352838501879052606084018890529351911692917f5a906361fbcec7017a235618b35c045125a85ca63b3272c041f779ce085dd770916080918190039190910190a35050505050565b6000600282604051602001808281526020019150506040516020818303038152906040526040518082805190602001908083835b602083106108065780518252601f1990920191602091820191016107e7565b51815160209384036101000a60001901801990921691161790526040519190930194509192505080830381855afa158015610845573d6000803e3d6000fd5b5050506040513d602081101561085a57600080fd5b505192915050565b600061086e838361119f565b9392505050565b6004546005549091565b610887610b85565b61089057600080fd5b6002546040516000916001600160a01b0316907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908390a3600280546001600160a01b0319169055565b8033600160008381526003602081905260409091206007015460ff169081111561090057fe5b1461093f57604051600160e51b62461bcd02815260040180806020018281038252604f815260200180611848604f913960600191505060405180910390fd5b6000828152600360205260409020600401546001600160a01b0382811691161461099d57604051600160e51b62461bcd0281526004018080602001828103825260418152602001806117716041913960600191505060405180910390fd5b600082815260036020526040902060010154428111156109f157604051600160e51b62461bcd02815260040180806020018281038252602d815260200180611897602d913960400191505060405180910390fd5b60008481526003602052604090206005810154600690910154610a219133916001600160a01b039091169061137d565b506000848152600360208190526040909120600701805460ff1916600183021790555060408051858152426020820152815133927fcc33ce18e322dc717363fea37fd6b19d05ce254cd48a950c2c173e35b244d7e3928290030190a250505050565b610a8c33611181565b1515600114610acf57604051600160e51b62461bcd0281526004018080602001828103825260258152602001806116216025913960400191505060405180910390fd5b600054610ae79033906001600160a01b031683611327565b600054610aff9033906001600160a01b0316836111cb565b50604051339082156108fc029083906000818181858888f19350505050158015610b2d573d6000803e3d6000fd5b506000546040805183815290516001600160a01b039092169133917fe67e5b35cdbd93a082969ccad448ff849669747b73ebe813df7a5d1100b1982a919081900360200190a350565b6002546001600160a01b031690565b6002546001600160a01b0316331490565b6001600160a01b038116610bde57604051600160e51b62461bcd0281526004018080602001828103825260238152602001806119136023913960400191505060405180910390fd5b81610c1d57604051600160e51b62461bcd0281526004018080602001828103825260538152602001806117b26053913960600191505060405180910390fd5b610c386001600160a01b03821633308563ffffffff6113b816565b610c4133611415565b50610c4d33828461137d565b506040805183815290516001600160a01b0383169133917f01527be533d184d44e3111afa7800fa60ced6e1b44bd025f8b457deb8ce0ce359181900360200190a35050565b610c9a610b85565b610ca357600080fd5b81610ce257604051600160e51b62461bcd0281526004018080602001828103825260438152602001806118056043913960600191505060405180910390fd5b81811015610d2457604051600160e51b62461bcd0281526004018080602001828103825260418152602001806119366041913960600191505060405180910390fd5b60408051808201909152603c928302808252919092026020909201829052600455600555565b808233600160008481526003602081905260409091206007015460ff1690811115610d7157fe5b14610db057604051600160e51b62461bcd02815260040180806020018281038252604f815260200180611977604f913960600191505060405180910390fd5b82600283604051602001808281526020019150506040516020818303038152906040526040518082805190602001908083835b60208310610e025780518252601f199092019160209182019101610de3565b51815160209384036101000a60001901801990921691161790526040519190930194509192505080830381855afa158015610e41573d6000803e3d6000fd5b5050506040513d6020811015610e5657600080fd5b505114610ead5760408051600160e51b62461bcd02815260206004820152601c60248201527f697352656465656d61626c653a20696e76616c69642073656372657400000000604482015290519081900360640190fd5b6000848152600360205260408120600701805460ff19166002179055610ed2856104ee565b6001811115610edd57fe5b1415610f2657600084815260036020526040808220600601549051339282156108fc02929190818181858888f19350505050158015610f20573d6000803e3d6000fd5b50610f5c565b60008481526003602052604090206006810154600590910154610f5c916001600160a01b0390911690339063ffffffff61123c16565b60008481526003602081815260409283902090910187905581518681524291810191909152808201879052905133917fd8c410238ddcadd482d857c422e62972bcd5b63864095af0499a9c530d777c8d919081900360600190a25050505050565b34610ffc57604051600160e51b62461bcd02815260040180806020018281038252604081526020018061169e6040913960400191505060405180910390fd5b61100533611415565b5060005461101e9033906001600160a01b03163461137d565b506000546040805134815290516001600160a01b039092169133917f01527be533d184d44e3111afa7800fa60ced6e1b44bd025f8b457deb8ce0ce35919081900360200190a3565b600061107182611181565b92915050565b61107f610b85565b61108857600080fd5b6110918161144c565b50565b6000806000806000806000806110a86115de565b60008a8152600360208181526040928390208351610100810185528154815260018201549281019290925260028101549382019390935282820154606082015260048301546001600160a01b03908116608083015260058401541660a0820152600683015460c0820152600783015490929160e084019160ff169081111561112c57fe5b600381111561113757fe5b815250509050806000015181602001518260400151836060015184608001518560a001518660c001518760e001519850985098509850985098509850985050919395975091939597565b6001600160a01b031660009081526001602052604090205460ff1690565b6001600160a01b0391821660009081526001602081815260408084209490951683529201909152205490565b6001600160a01b038084166000908152600160208181526040808420948716845293909101905290812054611206908363ffffffff6114bb16565b6001600160a01b038086166000908152600160208181526040808420948916845293820190529190209190915590509392505050565b604080516001600160a01b038416602482015260448082018490528251808303909101815260649091019091526020810180516001600160e01b0316600160e01b63a9059cbb021790526112919084906114d0565b505050565b600454603c8202908110156112df57604051600160e51b62461bcd02815260040180806020018281038252604f8152602001806118c4604f913960600191505060405180910390fd5b60055481111561132357604051600160e51b62461bcd0281526004018080602001828103825260508152602001806116de6050913960600191505060405180910390fd5b5050565b6000611333848461119f565b90508181101561137757604051600160e51b62461bcd02815260040180806020018281038252604381526020018061172e6043913960600191505060405180910390fd5b50505050565b6001600160a01b038084166000908152600160208181526040808420948716845293909101905290812054611206908363ffffffff6115c616565b604080516001600160a01b0385811660248301528416604482015260648082018490528251808303909101815260849091019091526020810180516001600160e01b0316600160e01b6323b872dd021790526113779085906114d0565b600061142082611181565b61051d57506001600160a01b03166000908152600160208190526040909120805460ff19168217905590565b6001600160a01b03811661145f57600080fd5b6002546040516001600160a01b038084169216907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e090600090a3600280546001600160a01b0319166001600160a01b0392909216919091179055565b6000828211156114ca57600080fd5b50900390565b6114e2826001600160a01b03166115d8565b6114eb57600080fd5b60006060836001600160a01b0316836040518082805190602001908083835b602083106115295780518252601f19909201916020918201910161150a565b6001836020036101000a0380198251168184511680821785525050505050509050019150506000604051808303816000865af19150503d806000811461158b576040519150601f19603f3d011682016040523d82523d6000602084013e611590565b606091505b50915091508161159f57600080fd5b805115611377578080602001905160208110156115bb57600080fd5b505161137757600080fd5b60008282018381101561086e57600080fd5b3b151590565b6040805161010081018252600080825260208201819052918101829052606081018290526080810182905260a0810182905260c081018290529060e08201529056fe776974686472617741737365743a207468652075736572206973206e6f7420616374697665696e6974696174653a207472616e7366657261626c6520616d6f756e742073686f756c6420626520626967676572207468616e2030776974686472617741737365743a20696e73756666696369656e742062616c616e63656465706f73697441737365743a2075736572206e6565647320746f207472616e736665722045544820666f722063616c6c696e672074686973206d6574686f645f76616c6964617465526566756e6454696d657374616d703a207468652074696d657374616d702073686f756c6420626520736d616c6c6572207468616e206d61782073776170206c69666574696d655f76616c696461746555736572417373657442616c616e63653a20706c656173652064656372656173652074686520616d6f756e7420616e642074727920616761696e6973526566756e6461626c653a206f6e6c792074686520696e69746961746f72206f662074686520737761702063616e2063616c6c2074686973206d6574686f646465706f73697441737365743a2075736572206e6565647320746f2066696c6c207472616e7366657261626c6520746f6b656e7320616d6f756e7420666f722063616c6c696e672074686973206d6574686f646368616e6765537761704c69666574696d654c696d6974733a206e65774d696e20616e64206e65774d61782073686f756c6420626520626967676572207468656e20306973526566756e6461626c653a20746865207377617020776974682074686973207365637265744861736820646f6573206e6f74206578697374206f7220686173206265656e2066696e69736865646973526566756e6461626c653a2074686520726566756e64206973206e6f7420617661696c61626c65206e6f775f76616c6964617465526566756e6454696d657374616d703a207468652074696d657374616d702073686f756c6420626520626967676572207468616e206d696e2073776170206c69666574696d656465706f73697441737365743a20696e76616c696420746f6b656e20616464726573736368616e6765537761704c69666574696d654c696d6974733a20746865206e65774d61782073686f756c6420626520626967676572207468656e206e65774d6178697352656465656d61626c653a20746865207377617020776974682074686973207365637265744861736820646f6573206e6f74206578697374206f7220686173206265656e2066696e697368656469734e6f74496e697469617465643a20746869732073656372657420686173682077617320616c726561647920757365642c20706c656173652075736520616e6f74686572206f6e65a165627a7a7230582014677da9167aa32ade611641af2c02905f3e1332059645321da4a59f49d0fdaa0029";

    public static final String FUNC_GETSWAPTYPE = "getSwapType";

    public static final String FUNC_WITHDRAWASSET = "withdrawAsset";

    public static final String FUNC_INITIATE = "initiate";

    public static final String FUNC_GETHASHOFSECRET = "getHashOfSecret";

    public static final String FUNC_GETUSERASSETAMOUNT = "getUserAssetAmount";

    public static final String FUNC_GETSWAPLIFETIMELIMITS = "getSwapLifetimeLimits";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_REFUND = "refund";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_DEPOSITASSET = "depositAsset";

    public static final String FUNC_CHANGESWAPLIFETIMELIMITS = "changeSwapLifetimeLimits";

    public static final String FUNC_REDEEM = "redeem";

    public static final String FUNC_DOESUSEREXIST = "doesUserExist";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_GETSWAPDATA = "getSwapData";

    public static final Event INITIATED_EVENT = new Event("Initiated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event REDEEMED_EVENT = new Event("Redeemed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event REFUNDED_EVENT = new Event("Refunded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event ASSETDEPOSITED_EVENT = new Event("AssetDeposited", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event ASSETWITHDRAWAL_EVENT = new Event("AssetWithdrawal", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
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

    public RemoteCall<TransactionReceipt> withdrawAsset(BigInteger amount, String token) {
        final Function function = new Function(
                FUNC_WITHDRAWASSET, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Address(token)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> initiate(byte[] secretHash, String asset, BigInteger amount, BigInteger refundTimestamp) {
        final Function function = new Function(
                FUNC_INITIATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(secretHash), 
                new org.web3j.abi.datatypes.Address(asset), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.generated.Uint256(refundTimestamp)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<byte[]> getHashOfSecret(byte[] secret) {
        final Function function = new Function(FUNC_GETHASHOFSECRET, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(secret)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteCall<BigInteger> getUserAssetAmount(String user, String asset) {
        final Function function = new Function(FUNC_GETUSERASSETAMOUNT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(user), 
                new org.web3j.abi.datatypes.Address(asset)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
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

    public RemoteCall<TransactionReceipt> refund(byte[] secretHash) {
        final Function function = new Function(
                FUNC_REFUND, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(secretHash)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> withdrawAsset(BigInteger amount) {
        final Function function = new Function(
                FUNC_WITHDRAWASSET, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(amount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<Boolean> isOwner() {
        final Function function = new Function(FUNC_ISOWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<TransactionReceipt> depositAsset(BigInteger amount, String token) {
        final Function function = new Function(
                FUNC_DEPOSITASSET, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Address(token)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> changeSwapLifetimeLimits(BigInteger newMin, BigInteger newMax) {
        final Function function = new Function(
                FUNC_CHANGESWAPLIFETIMELIMITS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(newMin), 
                new org.web3j.abi.datatypes.generated.Uint256(newMax)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> redeem(byte[] secret, byte[] secretHash) {
        final Function function = new Function(
                FUNC_REDEEM, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(secret), 
                new org.web3j.abi.datatypes.generated.Bytes32(secretHash)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> depositAsset(BigInteger weiValue) {
        final Function function = new Function(
                FUNC_DEPOSITASSET, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteCall<Boolean> doesUserExist(String user) {
        final Function function = new Function(FUNC_DOESUSEREXIST, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(user)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
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
            typedResponse.asset = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.secretHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.initTimestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.refundTimestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
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
                typedResponse.asset = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.secretHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.initTimestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.refundTimestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
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
            typedResponse.secretHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.refundTime = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
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
                typedResponse.secretHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.refundTime = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
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
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
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
                typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
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
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
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
                typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
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

        public String asset;

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

        public BigInteger amount;
    }

    public static class AssetWithdrawalEventResponse {
        public Log log;

        public String user;

        public String asset;

        public BigInteger amount;
    }
}
