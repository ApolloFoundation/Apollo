package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.exchange.model.ExpiredSwap;
import org.apache.commons.collections4.CollectionUtils;
import org.web3j.tuples.generated.Tuple2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ExpiredSwapMapper {
    public static List<ExpiredSwap> map(Tuple2<List<BigInteger>, List<byte[]>> data) {
        List<ExpiredSwap> swaps = new ArrayList<>();

        if (data == null || CollectionUtils.isEmpty(data.component1())) {
            return swaps;
        }

        for (int i = 0; i < data.component1().size(); i++) {
            swaps.add(new ExpiredSwap(Long.parseUnsignedLong(
                data.component1().get(i).toString()),
                (data.component2().get(i))
            ));
        }
        return swaps;
    }
}
