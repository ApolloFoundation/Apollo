package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DepositedOrderDetails;
import org.web3j.tuples.generated.Tuple4;

import java.math.BigInteger;

public class DepositedOrderDetailsMapper {


    public static DepositedOrderDetails map(Tuple4<Boolean, String, BigInteger, Boolean> responce) {
        return DepositedOrderDetails.builder()
            .created(responce.component1())
            .assetAddress(responce.component2())
            .amount(EthUtil.weiToEther(responce.component3()))
            .withdrawn(responce.component4())
                .build();
    }


}
