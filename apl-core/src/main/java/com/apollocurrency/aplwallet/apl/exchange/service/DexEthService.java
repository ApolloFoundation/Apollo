package com.apollocurrency.aplwallet.apl.exchange.service;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.apollocurrency.aplwallet.apl.exchange.dao.EthGasStationInfoDao;
import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Singleton
public class DexEthService {
    private static Integer attempts = 5;
    private static String ETH_GAS_INFO_KEY = "eth_gas_info";

    private EthGasStationInfoDao ethGasStationInfoDao;

    private LoadingCache<String, Object> cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(2, TimeUnit.MINUTES)
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
            ethGasInfo = ethGasStationInfoDao.getEthPriceInfo();
            if (ethGasInfo != null) {
                return ethGasInfo;
            }
            counter++;
        }

        return ethGasInfo;
    }

}
