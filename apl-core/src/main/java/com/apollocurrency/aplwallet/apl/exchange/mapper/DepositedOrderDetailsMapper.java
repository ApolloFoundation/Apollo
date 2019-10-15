package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.exchange.model.DepositedOrderDetails;
import org.web3j.tuples.generated.Tuple4;

import java.math.BigInteger;

public class DepositedOrderDetailsMapper {


    public static DepositedOrderDetails map(Tuple4<Boolean, String, BigInteger, Boolean> responce) {
        return DepositedOrderDetails.builder()
                .created(responce.getValue1())
                .assetAddress(responce.getValue2())
                .amount(responce.getValue3())
                .withdrawn(responce.getValue4())
                .build();
    }


}
