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
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple8;
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
    private static final String BINARY = "600080546001600160a01b0319908116825560c06040819052610258608081905262ed4e0060a0819052600591909155600655600280549092163317918290556001600160a01b039190911691907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908290a36121a7806100816000396000f3fe6080604052600436106101355760003560e01c80637dc4da2e116100ab578063a4cdbd141161006f578063a4cdbd1414610563578063b6b55f2514610593578063ecb41054146105b0578063eda1122c146105e3578063f2fde38b1461060d578063f7b3ae2f1461064057610135565b80637dc4da2e1461044d5780638da5cb5b146104925780638dbdbe6d146104c35780638f32d59b146105025780639fe21fc41461052b57610135565b8063655ac12c116100fd578063655ac12c146103275780636d72dc381461035a578063715018a6146103885780637249fbb61461039d57806377b0dd70146103c75780637c68bebe1461041a57610135565b806318df00381461013a5780632e1a7d4d146101885780634861f39b146101b457806354ef481d1461021f578063571694cd146102eb575b600080fd5b34801561014657600080fd5b506101646004803603602081101561015d57600080fd5b50356106e3565b6040518082600181111561017457fe5b60ff16815260200191505060405180910390f35b34801561019457600080fd5b506101b2600480360360208110156101ab57600080fd5b50356106f6565b005b3480156101c057600080fd5b506101ed600480360360408110156101d757600080fd5b50803590602001356001600160a01b0316610873565b6040805194151585526001600160a01b0390931660208501528383019190915215156060830152519081900360800190f35b34801561022b57600080fd5b506102526004803603602081101561024257600080fd5b50356001600160a01b03166108ae565b604051808060200180602001838103835285818151815260200191508051906020019060200280838360005b8381101561029657818101518382015260200161027e565b50505050905001838103825284818151815260200191508051906020019060200280838360005b838110156102d55781810151838201526020016102bd565b5050505090500194505050505060405180910390f35b3480156102f757600080fd5b506103156004803603602081101561030e57600080fd5b50356109f6565b60408051918252519081900360200190f35b34801561033357600080fd5b506102526004803603602081101561034a57600080fd5b50356001600160a01b0316610a01565b34801561036657600080fd5b5061036f610bc7565b6040805192835260208301919091528051918290030190f35b34801561039457600080fd5b506101b2610bd1565b3480156103a957600080fd5b506101b2600480360360208110156103c057600080fd5b5035610c2c565b3480156103d357600080fd5b506101b2600480360360c08110156103ea57600080fd5b508035906020810135906001600160a01b0360408201358116916060810135916080820135169060a00135610df1565b34801561042657600080fd5b506103156004803603602081101561043d57600080fd5b50356001600160a01b0316610e10565b34801561045957600080fd5b506101b26004803603608081101561047057600080fd5b508035906020810135906001600160a01b036040820135169060600135610e2f565b34801561049e57600080fd5b506104a76110c8565b604080516001600160a01b039092168252519081900360200190f35b3480156104cf57600080fd5b506101b2600480360360608110156104e657600080fd5b50803590602081013590604001356001600160a01b03166110d7565b34801561050e57600080fd5b506105176111fc565b604080519115158252519081900360200190f35b6101b26004803603608081101561054157600080fd5b508035906020810135906001600160a01b03604082013516906060013561120d565b34801561056f57600080fd5b506101b26004803603604081101561058657600080fd5b5080359060200135611228565b6101b2600480360360208110156105a957600080fd5b50356112da565b3480156105bc57600080fd5b50610517600480360360208110156105d357600080fd5b50356001600160a01b0316611395565b3480156105ef57600080fd5b506101b26004803603602081101561060657600080fd5b50356113a0565b34801561061957600080fd5b506101b26004803603602081101561063057600080fd5b50356001600160a01b03166115c0565b34801561064c57600080fd5b5061066a6004803603602081101561066357600080fd5b50356115dd565b60405180898152602001888152602001878152602001868152602001856001600160a01b03166001600160a01b03168152602001846001600160a01b03166001600160a01b031681526020018381526020018260038111156106c857fe5b60ff1681526020019850505050505050505060405180910390f35b60006106ee826116e3565b90505b919050565b6106ff3361171a565b1515600114610755576040805162461bcd60e51b815260206004820181905260248201527f77697468647261773a207468652075736572206973206e6f7420616374697665604482015290519081900360640190fd5b61075d611c8b565b6107678233611738565b6020810151604082015160608301519293509091156107b75760405162461bcd60e51b815260040180806020018281038252604a815260200180611d3d604a913960600191505060405180910390fd5b6107c184336117aa565b506000546001600160a01b038381169116141561080b57604051339082156108fc029083906000818181858888f19350505050158015610805573d6000803e3d6000fd5b50610825565b6108256001600160a01b038316338363ffffffff6117e116565b604080518581526020810183905281516001600160a01b0385169233927f44caac6b5921bbf9e5a91b5c88bae1355a24b50a810dfd697818636334049a1e929081900390910190a350505050565b600080600080610881611c8b565b61088b8787611738565b805160208201516040830151606090930151919a90995091975095509350505050565b606080600060016000856001600160a01b03166001600160a01b0316815260200190815260200160002060010154905080604051908082528060200260200182016040528015610908578160200160208202803883390190505b50925080604051908082528060200260200182016040528015610935578160200160208202803883390190505b5091506000805b8281116109ee576001600160a01b03861660009081526001602090815260408083208484526002810183528184205480855260039091019092529091205460ff16156109e5578086848151811061098f57fe5b6020908102919091018101919091526001600160a01b038816600090815260018083526040808320858452600301909352919020015485518690859081106109d357fe5b60209081029190910101526001909201915b5060010161093c565b505050915091565b60006106ee82611833565b6001600160a01b0381166000908152600460209081526040918290208054600190910154835181815281840281019093019093526060928392818015610a51578160200160208202803883390190505b50925080604051908082528060200260200182016040528015610a7e578160200160208202803883390190505b5093506000805b838111610bbc57610a94611cb2565b6001600160a01b0380891660009081526004602081815260408084208785526002908101835281852054855260038084529482902082516101408101845281548152600182015494810194909452908101549183019190915280840154606083015291820154841660808201526005820154841660a0820152600682015490931660c0840152600781015460e084015260088101546101008401526009810154909161012084019160ff1690811115610b4957fe5b6003811115610b5457fe5b905250905060018161012001516003811115610b6c57fe5b1415610bb3578060e00151878481518110610b8357fe5b602002602001018181525050806101000151868481518110610ba157fe5b60209081029190910101526001909201915b50600101610a85565b509295939450505050565b6005546006549091565b610bd96111fc565b610be257600080fd5b6002546040516000916001600160a01b0316907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908390a3600280546001600160a01b0319169055565b8033600160008381526003602081905260409091206009015460ff1690811115610c5257fe5b14610c8e5760405162461bcd60e51b815260040180806020018281038252604f815260200180611f61604f913960600191505060405180910390fd5b6000828152600360205260409020600401546001600160a01b03828116911614610ce95760405162461bcd60e51b8152600401808060200182810382526041815260200180611e256041913960600191505060405180910390fd5b60008281526003602052604090206001015442811115610d3a5760405162461bcd60e51b815260040180806020018281038252602d815260200180611fb0602d913960400191505060405180910390fd5b600084815260036020819052604090912060098101805460ff191690921790915560088101546006820154600790920154610d829233916001600160a01b03909116906118e2565b5033600081815260046020908152604080832060010180546000190190558783526003825291829020600801548251908152908101879052428183015290517f222b84eaf6103dc4589b81de12d7b2f6a1d0b45039451d2af236473910c4a2409181900360600190a250505050565b610dfc8686866110d7565b610e0886848484610e2f565b505050505050565b6001600160a01b03166000908152600160208190526040909120015490565b826000808281526003602081905260409091206009015460ff1690811115610e5357fe5b14610e8f5760405162461bcd60e51b815260040180806020018281038252604981526020018061212a6049913960600191505060405180910390fd5b610e9b82603c02611986565b610ea3611c8b565b610ead8633611738565b80519091501515600114610ef25760405162461bcd60e51b815260040180806020018281038252603e81526020018061202c603e913960400191505060405180910390fd5b606081015115610f335760405162461bcd60e51b815260040180806020018281038252602f815260200180611ea1602f913960400191505060405180910390fd5b6040810151610f735760405162461bcd60e51b815260040180806020018281038252604e815260200180611dd7604e913960600191505060405180910390fd5b610f7d86336117aa565b50602081810151600087815260039092526040918290206006810180546001600160a01b0319166001600160a01b039093169290921790915590820151600782015560090180546001919060ff1916828002179055506000858152600360209081526040808320600480820180546001600160a01b0319908116339081179092556005840180546001600160a01b038d811691909316811790915560028086018e9055600886018f905542808755603c8d028101600197880155848a5294885286892080548a529081018852978690208d90558754850188558785018054909501909455878601518886015186518f81529788018e905287870194909452606087018b90526080870193909352935191909316939192917f91acd19eaf946e9dde3bbe5c8b1d0c4e95684f4bbce98fcd2c53e8777af7c636919081900360a00190a450505050505050565b6002546001600160a01b031690565b33600081815260016020908152604080832087845260030190915290205484919060ff16156111375760405162461bcd60e51b8152600401808060200182810382526038815260200180611d056038913960400191505060405180910390fd5b6001600160a01b038316611192576040805162461bcd60e51b815260206004820152601e60248201527f6465706f7369743a20696e76616c696420746f6b656e20616464726573730000604482015290519081900360640190fd5b836111ce5760405162461bcd60e51b815260040180806020018281038252604e815260200180611f13604e913960600191505060405180910390fd5b6111e96001600160a01b03841633308763ffffffff611a0816565b6111f585338587611a62565b5050505050565b6002546001600160a01b0316331490565b611216846112da565b61122284848484610e2f565b50505050565b6112306111fc565b61123957600080fd5b816112755760405162461bcd60e51b8152600401808060200182810382526043815260200180611ed06043913960600191505060405180910390fd5b818110156112b45760405162461bcd60e51b815260040180806020018281038252604181526020018061206a6041913960600191505060405180910390fd5b60408051808201909152603c928302808252919092026020909201829052600555600655565b33600081815260016020908152604080832085845260030190915290205482919060ff161561133a5760405162461bcd60e51b8152600401808060200182810382526038815260200180611d056038913960400191505060405180910390fd5b346113765760405162461bcd60e51b815260040180806020018281038252603b815260200180611e66603b913960400191505060405180910390fd5b60005461139090849033906001600160a01b031634611a62565b505050565b60006106ee8261171a565b8060006113ac82611833565b9050600160008281526003602081905260409091206009015460ff16908111156113d257fe5b1461140e5760405162461bcd60e51b815260040180806020018281038252604f8152602001806120db604f913960600191505060405180910390fd5b60008181526003602052604090206001015442811161145e5760405162461bcd60e51b81526004018080602001828103825260308152602001806120ab6030913960400191505060405180910390fd5b600061146985611833565b600081815260036020526040812060098101805460ff19166002179055600501549192506001600160a01b03909116906114a2836116e3565b60018111156114ad57fe5b14156115025760008281526003602052604080822060070154905183926001600160a01b0384169280156108fc02929091818181858888f193505050501580156114fb573d6000803e3d6000fd5b5050611538565b60008281526003602052604090206007810154600690910154611538916001600160a01b0390911690839063ffffffff6117e116565b60008281526003602081815260408084209283018a90556004928301546001600160a01b039081168552928252928390206001018054600019019055825185815242918101919091528083018990529151908316917fd8c410238ddcadd482d857c422e62972bcd5b63864095af0499a9c530d777c8d919081900360600190a2505050505050565b6115c86111fc565b6115d157600080fd5b6115da81611ae2565b50565b6000806000806000806000806115f1611cb2565b60008a8152600360208181526040928390208351610140810185528154815260018201549281019290925260028101549382019390935282820154606082015260048301546001600160a01b0390811660808301526005840154811660a083015260068401541660c0820152600783015460e08201526008830154610100820152600983015490929161012084019160ff169081111561168d57fe5b600381111561169857fe5b815250509050806000015181602001518260400151836060015184608001518560c001518660e001518761012001519850985098509850985098509850985050919395975091939597565b60008054828252600360205260408220600601546001600160a01b0390811691161415611712575060006106f1565b5060016106f1565b6001600160a01b031660009081526001602052604090205460ff1690565b611740611c8b565b506001600160a01b03908116600090815260016020818152604080842095845260039095018152918490208451608081018652815460ff80821615158352610100909104909516938101939093529081015493820193909352600290920154161515606082015290565b6001600160a01b0316600090815260016020818152604080842094845260039094019052919020600201805460ff19168217905590565b604080516001600160a01b038416602482015260448082018490528251808303909101815260649091019091526020810180516001600160e01b031663a9059cbb60e01b179052611390908490611b51565b6000600282604051602001808281526020019150506040516020818303038152906040526040518082805190602001908083835b602083106118865780518252601f199092019160209182019101611867565b51815160209384036101000a60001901801990921691161790526040519190930194509192505080830381855afa1580156118c5573d6000803e3d6000fd5b5050506040513d60208110156118da57600080fd5b505192915050565b6001600160a01b039283166000908152600160208181526040808420808401548552600280820184528286208a90558251608081018452858152978916888501908152888401978852606089018781529a87526003909201909352932094518554935160ff1994851691151591909117610100600160a81b031916610100919097160295909517845591518383015593519190920180549093169015151790915590565b6005548110156119c75760405162461bcd60e51b815260040180806020018281038252604f815260200180611fdd604f913960600191505060405180910390fd5b6006548111156115da5760405162461bcd60e51b8152600401808060200182810382526050815260200180611d876050913960600191505060405180910390fd5b604080516001600160a01b0385811660248301528416604482015260648082018490528251808303909101815260849091019091526020810180516001600160e01b03166323b872dd60e01b179052611222908590611b51565b611a6b83611c47565b50611a78848484846118e2565b506001600160a01b038084166000818152600160208181526040928390208201805490920190915581518881529081018590528151938616937f2389aeb1847d9139fac317075af2ac7ace8586c66ea0fa893970980ab2be4356929181900390910190a350505050565b6001600160a01b038116611af557600080fd5b6002546040516001600160a01b038084169216907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e090600090a3600280546001600160a01b0319166001600160a01b0392909216919091179055565b611b63826001600160a01b0316611c85565b611b6c57600080fd5b60006060836001600160a01b0316836040518082805190602001908083835b60208310611baa5780518252601f199092019160209182019101611b8b565b6001836020036101000a0380198251168184511680821785525050505050509050019150506000604051808303816000865af19150503d8060008114611c0c576040519150601f19603f3d011682016040523d82523d6000602084013e611c11565b606091505b509150915081611c2057600080fd5b80511561122257808060200190516020811015611c3c57600080fd5b505161122257600080fd5b6000611c528261171a565b611c7d576001600160a01b0382166000908152600160208190526040909120805460ff191690911790555b506001919050565b3b151590565b60408051608081018252600080825260208201819052918101829052606081019190915290565b6040805161014081018252600080825260208201819052918101829052606081018290526080810182905260a0810182905260c0810182905260e081018290526101008101829052906101208201529056fe6f72646572496449734e6f744465706f73697465643a207573657220616c7265616479206465706f7369742074686973206f72646572496477697468647261773a2074686973206f7264657220496420686173206265656e20616c72656164792077697468647261776e206f722077616974696e6720666f722074686520737761705f76616c6964617465526566756e6454696d657374616d703a207468652074696d657374616d702073686f756c6420626520736d616c6c6572207468616e206d61782073776170206c69666574696d65696e6974696174653a2074686973206f7264657220496420686173206265656e2077697468647261776e2c2066696e6973686564206f722077616974696e6720666f72207468652072656465656d6973526566756e6461626c653a206f6e6c792074686520696e69746961746f72206f662074686520737761702063616e2063616c6c2074686973206d6574686f646465706f7369743a2075736572206e6565647320746f207472616e736665722045544820666f722063616c6c696e672074686973206d6574686f64696e6974696174653a2074686973206f72646572206465706f73697420686173206265656e2077697468647261776e6368616e6765537761704c69666574696d654c696d6974733a206e65774d696e20616e64206e65774d61782073686f756c6420626520626967676572207468656e20306465706f7369743a2075736572206e6565647320746f2066696c6c207472616e7366657261626c6520746f6b656e7320616d6f756e7420666f722063616c6c696e672074686973206d6574686f646973526566756e6461626c653a20746865207377617020776974682074686973207365637265744861736820646f6573206e6f74206578697374206f7220686173206265656e2066696e69736865646973526566756e6461626c653a2074686520726566756e64206973206e6f7420617661696c61626c65206e6f775f76616c6964617465526566756e6454696d657374616d703a207468652074696d657374616d702073686f756c6420626520626967676572207468616e206d696e2073776170206c69666574696d65696e6974696174653a2074686973206f7264657220496420686173206e6f74206265656e206372656174656420616e64206465706f7369746564207965746368616e6765537761704c69666574696d654c696d6974733a20746865206e65774d61782073686f756c6420626520626967676572207468656e206e65774d6178697352656465656d61626c653a207468652072656465656d20697320636c6f73656420666f7220746869732073776170697352656465656d61626c653a20746865207377617020776974682074686973207365637265744861736820646f6573206e6f74206578697374206f7220686173206265656e2066696e697368656469734e6f74496e697469617465643a20746869732073656372657420686173682077617320616c726561647920757365642c20706c656173652075736520616e6f74686572206f6e65a265627a7a723058207469ca3999cfddee3da8b02d3f9f83971bcf437da4d38c1695fad5db00947e3f64736f6c634300050a0032";

    public static final String FUNC_GETSWAPTYPE = "getSwapType";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final String FUNC_GETDEPOSITEDORDERDETAILS = "getDepositedOrderDetails";

    public static final String FUNC_GETUSERFILLEDDEPOSITS = "getUserFilledDeposits";

    public static final String FUNC_GETHASHOFSECRET = "getHashOfSecret";

    public static final String FUNC_GETUSERFILLEDORDERS = "getUserFilledOrders";

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

    public RemoteCall<Tuple2<List<BigInteger>, List<BigInteger>>> getUserFilledDeposits(String user) {
        final Function function = new Function(FUNC_GETUSERFILLEDDEPOSITS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(user)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint256>>() {
                }, new TypeReference<DynamicArray<Uint256>>() {
                }));
        return new RemoteCall<Tuple2<List<BigInteger>, List<BigInteger>>>(
                new Callable<Tuple2<List<BigInteger>, List<BigInteger>>>() {
                    @Override
                    public Tuple2<List<BigInteger>, List<BigInteger>> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<List<BigInteger>, List<BigInteger>>(
                                convertToNative((List<Uint256>) results.get(0).getValue()),
                                convertToNative((List<Uint256>) results.get(1).getValue()));
                    }
                });
    }

    public RemoteCall<byte[]> getHashOfSecret(byte[] secret) {
        final Function function = new Function(FUNC_GETHASHOFSECRET, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(secret)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteCall<Tuple2<List<BigInteger>, List<BigInteger>>> getUserFilledOrders(String user) {
        final Function function = new Function(FUNC_GETUSERFILLEDORDERS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(user)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint256>>() {
                }, new TypeReference<DynamicArray<Uint256>>() {
                }));
        return new RemoteCall<Tuple2<List<BigInteger>, List<BigInteger>>>(
                new Callable<Tuple2<List<BigInteger>, List<BigInteger>>>() {
                    @Override
                    public Tuple2<List<BigInteger>, List<BigInteger>> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<List<BigInteger>, List<BigInteger>>(
                                convertToNative((List<Uint256>) results.get(0).getValue()),
                                convertToNative((List<Uint256>) results.get(1).getValue()));
                    }
                });
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
