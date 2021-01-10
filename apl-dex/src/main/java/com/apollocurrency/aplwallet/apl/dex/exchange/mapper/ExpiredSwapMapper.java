package com.apollocurrency.aplwallet.apl.dex.exchange.mapper;

import com.apollocurrency.aplwallet.apl.dex.exchange.model.ExpiredSwap;
import com.apollocurrency.aplwallet.apl.util.AplCollectionUtils;
import org.web3j.tuples.generated.Tuple2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ExpiredSwapMapper {
    public static List<ExpiredSwap> map(Tuple2<List<BigInteger>, List<byte[]>> data) {
        List<ExpiredSwap> swaps = new ArrayList<>();

        if (data == null || AplCollectionUtils.isEmpty(data.component1())) {
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
