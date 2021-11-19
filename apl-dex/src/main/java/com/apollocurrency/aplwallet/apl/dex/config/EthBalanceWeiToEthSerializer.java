package com.apollocurrency.aplwallet.apl.dex.config;

import com.apollocurrency.aplwallet.apl.dex.eth.utils.EthUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigInteger;

public class EthBalanceWeiToEthSerializer extends JsonSerializer<BigInteger> {
    @Override
    public void serialize(BigInteger value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(EthUtil.weiToEther(value).toString());
    }
}
