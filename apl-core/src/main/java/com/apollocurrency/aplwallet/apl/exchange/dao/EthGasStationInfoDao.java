package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

@Singleton
public class EthGasStationInfoDao {

    public EthGasInfo getEthPriceInfo() throws IOException {
        EthGasInfo ethGasInfo = null;
        HttpURLConnection con = null;
        try {
            URL url = new URL(Constants.ETH_GAS_INFO_URL);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (Reader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
                    ethGasInfo = new ObjectMapper()
                            .readerFor(EthGasInfo.class)
                            .readValue(reader);
                }
            }
        } finally {
            con.disconnect();
        }
        return ethGasInfo;
    }

}
