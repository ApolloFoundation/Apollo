package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.EthDepositInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.EthDepositsWithOffset;
import org.apache.commons.collections4.CollectionUtils;
import org.web3j.tuples.generated.Tuple4;

import java.math.BigInteger;
import java.util.List;

public class UserEthDepositInfoMapper {


    public static EthDepositsWithOffset map(Tuple4<List<BigInteger>, List<BigInteger>, List<BigInteger>, BigInteger> data) {
        EthDepositsWithOffset ethDepositsWithOffset = new EthDepositsWithOffset();

        if (data == null || CollectionUtils.isEmpty(data.getValue1())) {
            return ethDepositsWithOffset;
        }

        for (int i = 0; i < data.component1().size(); i++) {
            ethDepositsWithOffset.getDeposits().add(new EthDepositInfo(Long.parseUnsignedLong(
                data.component1().get(i).toString()),
                EthUtil.weiToEther(data.component2().get(i)),
                data.component3().get(i).longValue()
                    )
            );
        }
        ethDepositsWithOffset.setOffset(data.component4().longValue());
        return ethDepositsWithOffset;
    }

}
