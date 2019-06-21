package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

@Singleton
public class EthGasStationInfoDao {
    private static final Logger LOG = LoggerFactory.getLogger(EthGasStationInfoDao.class);
    private static String ETH_GAS_INFO_URL = "https://ethgasstation.info/json/ethgasAPI.json";

    public EthGasInfo getEthPriceInfo(){
        EthGasInfo ethGasInfo = null;
        HttpURLConnection con = null;

        try{
            URL url = new URL(ETH_GAS_INFO_URL);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (Reader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
                    ethGasInfo = new ObjectMapper()
                            .readerFor(EthGasInfo.class)
                            .readValue(reader);
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            con.disconnect();
        }
        return ethGasInfo;
    }



}
