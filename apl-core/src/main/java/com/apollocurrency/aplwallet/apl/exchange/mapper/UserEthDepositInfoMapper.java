package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.UserEthDepositInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.web3j.tuples.generated.Tuple3;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class UserEthDepositInfoMapper {


    public static List<UserEthDepositInfo> map(Tuple3<List<BigInteger>, List<BigInteger>, List<BigInteger>> data) {
        List<UserEthDepositInfo> userDeposits = new ArrayList<>();

        if (data == null || CollectionUtils.isEmpty(data.getValue1())) {
            return userDeposits;
        }

        for (int i = 0; i < data.component1().size(); i++) {
            userDeposits.add(new UserEthDepositInfo(Long.parseUnsignedLong(
                data.component1().get(i).toString()),
                EthUtil.weiToEther(data.component2().get(i)),
                data.component3().get(i).longValue()
                    )
            );
        }

        return userDeposits;
    }

}
