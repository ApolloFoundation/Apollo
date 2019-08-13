package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.exchange.dao.EthGasStationInfoDao;
import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class DexEthService {
    private static Integer attempts = 5;
    private static String ETH_GAS_INFO_KEY = "eth_gas_info";

    private EthGasStationInfoDao ethGasStationInfoDao;

    private LoadingCache<String, Object> cache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterWrite(4, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<>() {
                        public EthGasInfo load(String id) throws InvalidCacheLoadException {
                            EthGasInfo ethGasInfo = initEthPriceInfo();
                            if(ethGasInfo==null){
                                throw new InvalidCacheLoadException("Value can't be null");
                            }
                            return ethGasInfo;
                        }
                    }
            );

    @Inject
    public DexEthService(EthGasStationInfoDao ethGasStationInfoDao) {
        this.ethGasStationInfoDao = ethGasStationInfoDao;
    }

    public EthGasInfo getEthPriceInfo() throws ExecutionException {
        return (EthGasInfo) cache.get(ETH_GAS_INFO_KEY);
    }

    private EthGasInfo initEthPriceInfo(){
        EthGasInfo ethGasInfo = null;
        Integer counter = 0;
        while (counter < attempts) {
            try {
                ethGasInfo = ethGasStationInfoDao.getEthPriceInfo();
            } catch (Exception e) {
                log.error("Attempt " + counter +":" + e.getMessage(), e);
            }

            if (ethGasInfo != null) {
                return ethGasInfo;
            }
            counter++;
        }

        return null;
    }

}
