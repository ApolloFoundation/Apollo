package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.exchange.model.ExpiredSwap;
import com.apollocurrency.aplwallet.apl.exchange.model.ExpiredSwapsWithOffset;
import org.apache.commons.collections4.CollectionUtils;
import org.web3j.tuples.generated.Tuple3;

import java.math.BigInteger;
import java.util.List;

public class ExpiredSwapMapper {
    public static ExpiredSwapsWithOffset map(Tuple3<List<BigInteger>, List<byte[]>, BigInteger> data) {
        ExpiredSwapsWithOffset swapsWithOffset = new ExpiredSwapsWithOffset();

        if (data == null || CollectionUtils.isEmpty(data.component1())) {
            return swapsWithOffset;
        }

        for (int i = 0; i < data.component1().size(); i++) {
            swapsWithOffset.getSwaps().add(new ExpiredSwap(Long.parseUnsignedLong(
                data.component1().get(i).toString()),
                    (data.component2().get(i))));
        }
        swapsWithOffset.setOffset(data.component3().longValue());
        return swapsWithOffset;
    }
}
