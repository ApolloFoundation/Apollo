package com.apollocurrency.aplwallet.apl.dex.eth.service;

import com.apollocurrency.aplwallet.apl.dex.eth.model.EthGasInfo;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class DexEthService {
    private static Integer ATTEMPTS = 5;
    private static String ETH_GAS_INFO_KEY = "eth_gas_info";

    private EthGasStationInfoService ethGasStationInfoService;

    private LoadingCache<String, Object> cache = CacheBuilder.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(3, TimeUnit.MINUTES)
        .build(
            new CacheLoader<>() {
                public EthGasInfo load(String id) throws InvalidCacheLoadException {
                    EthGasInfo ethGasInfo = initEthPriceInfo();
                    if (ethGasInfo == null) {
                        throw new InvalidCacheLoadException("Value can't be null");
                    }
                    return ethGasInfo;
                }
            }
        );

    @Inject
    public DexEthService(EthGasStationInfoService ethGasStationInfoService) {
        this.ethGasStationInfoService = ethGasStationInfoService;
    }

    public EthGasInfo getEthPriceInfo() throws ExecutionException {
        return (EthGasInfo) cache.get(ETH_GAS_INFO_KEY);
    }

    private EthGasInfo initEthPriceInfo() {
        EthGasInfo ethGasInfo;
        Integer counter = 0;
        while (counter < ATTEMPTS) {
            try {
                ethGasInfo = ethGasStationInfoService.getEthPriceInfo();

                if (ethGasInfo != null) {
                    log.info("Received new gas price info from ETH Gas Station {}", ethGasInfo);
                    return ethGasInfo;
                }
            } catch (Exception e) {
                log.error("(Gas Station) Attempt " + counter + ":" + e.getMessage(), e);
            }
            try {
                ethGasInfo = ethGasStationInfoService.getEthChainPriceInfo();

                if (ethGasInfo != null) {
                    log.info("Received new gas price info from Etherchain {}", ethGasInfo);
                    return ethGasInfo;
                }
            } catch (Exception e) {
                log.error("(Eth Chain) Attempt " + counter + ":" + e.getMessage(), e);
            }

            counter++;
        }

        return null;
    }

}
