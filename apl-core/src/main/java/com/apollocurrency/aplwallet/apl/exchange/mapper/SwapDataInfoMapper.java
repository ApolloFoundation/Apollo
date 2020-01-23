package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.SwapDataInfo;
import org.web3j.tuples.generated.Tuple9;

import java.math.BigInteger;

public class SwapDataInfoMapper {


    public static SwapDataInfo map(Tuple9<BigInteger, BigInteger, byte[], byte[], String, String, String, BigInteger, BigInteger> response) {
        return SwapDataInfo.builder()
            .timeStart(response.component1().longValue())
            .timeDeadLine(response.component2().longValue())
            .secretHash(response.component3())
            .secret(isEmpty(response.component4()) ? null : response.getValue4())
            .addressFrom(response.component5())
            .addressTo(response.component6())
            .addressAsset(response.component7())
            .status(response.component9().intValue())
            .amount(EthUtil.weiToEther(response.component8()))
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
