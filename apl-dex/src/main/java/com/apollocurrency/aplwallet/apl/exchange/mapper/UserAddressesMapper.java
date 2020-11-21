package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.exchange.model.UserAddressesWithOffset;
import org.web3j.tuples.generated.Tuple2;

import java.math.BigInteger;
import java.util.List;

public class UserAddressesMapper {
    public static UserAddressesWithOffset map(Tuple2<List<String>, BigInteger> data) {
        return new UserAddressesWithOffset(data.component1(), data.component2().longValue());
    }

}
