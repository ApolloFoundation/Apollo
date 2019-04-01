package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeBalances;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeOrder;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DexService {
    private static final Logger LOG = LoggerFactory.getLogger(DexService.class);

    private EthereumWalletService ethereumWalletService = CDI.current().select(EthereumWalletService.class).get();

    public ExchangeBalances getBalances(String ethAddress, String paxAddress){

        ExchangeBalances balances = new ExchangeBalances();
        try{
            if (!StringUtils.isBlank(ethAddress)) {
                balances.setBalanceETH(ethereumWalletService.getBalanceWei(ethAddress));
            }
        } catch (Exception ex){
            LOG.error(ex.getMessage(), ex.getCause().getMessage());
        }

        try{
            if (!StringUtils.isBlank(paxAddress)) {
                balances.setBalancePAX(ethereumWalletService.getPaxBalanceWei(paxAddress));
            }
        } catch (Exception ex){
            LOG.error(ex.getMessage(), ex.getCause().getMessage());
        }

        return balances;
    }

    public List<ExchangeOrder> getHistory(String account, String pair, String type){
        return new ArrayList<>();
    }

    public List<ExchangeOrder> getOffers(String account, String pair, String type, BigDecimal minAskPrice, BigDecimal maxBidPrice){
        return new ArrayList<>();
    }
    public ExchangeOrder getOrderByID(Long orderID){
        return null;
    }

    public boolean widthraw(String account,String secretPhrase,BigDecimal amount,String address,String cryptocurrency){
        return false;
    }


}
