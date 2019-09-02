package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.exchange.model.SwapDataInfo;
import org.web3j.tuples.generated.Tuple8;

import java.math.BigInteger;

public class SwapDataInfoMapper {


    public static SwapDataInfo map(Tuple8<BigInteger, BigInteger, byte[], byte[], String, String, BigInteger, BigInteger> response){
        return SwapDataInfo.builder()
                .timeStart(response.getValue1().longValue())
                .timeDeadLine(response.getValue2().longValue())
                .secretHash(response.getValue3())
                .secret(isEmpty(response.getValue4()) ? null : response.getValue4())
                .address(response.getValue5())
                .address2(response.getValue6())
                .status(response.getValue7().intValue())
                .amount(response.getValue8())
                .build();
    }

    private static boolean isEmpty(byte[] array){
        if(array == null){
            return true;
        }
        for (byte b : array) {
            if(b != 0) {
                return false;
            }
        }
        return true;
    }

}
