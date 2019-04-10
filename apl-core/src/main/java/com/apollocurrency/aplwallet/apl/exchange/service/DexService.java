package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeBalances;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DexService {
    private static final Logger LOG = LoggerFactory.getLogger(DexService.class);

    private EthereumWalletService ethereumWalletService;
    private DexOfferDao dexOfferDao;
    private EpochTime epochTime;

    @Inject
    public DexService(EthereumWalletService ethereumWalletService, DexOfferDao dexOfferDao, EpochTime epochTime) {
        this.ethereumWalletService = ethereumWalletService;
        this.dexOfferDao = dexOfferDao;
        this.epochTime = epochTime;
    }



    public void saveOffer (DexOffer offer){
        dexOfferDao.save(offer);
    }

    public List<DexOffer> getOffers(OfferType type, DexCurrencies offerCur, DexCurrencies pairCur, BigDecimal minAskPrice, BigDecimal maxBidPrice){
        Integer currentTime = epochTime.getEpochTime();

        DexOfferDBRequest dexOfferDBRequest = new DexOfferDBRequest(type, currentTime, offerCur, pairCur, minAskPrice, maxBidPrice);

        return dexOfferDao.getOffers(dexOfferDBRequest);
    }

    public ExchangeBalances getBalances(String ethAddress, String paxAddress){
        ExchangeBalances balances = new ExchangeBalances();
        try{
            if (!StringUtils.isBlank(ethAddress)) {
                balances.setBalanceETH(ethereumWalletService.getBalanceWei(ethAddress));
            }
        } catch (Exception ex){
            LOG.error(ex.getMessage());
        }

        try{
            if (!StringUtils.isBlank(paxAddress)) {
                balances.setBalancePAX(ethereumWalletService.getPaxBalanceWei(paxAddress));
            }
        } catch (Exception ex){
            LOG.error(ex.getMessage());
        }

        return balances;
    }

    public List<ExchangeOrder> getHistory(String account, String pair, String type){
        return new ArrayList<>();
    }


    public ExchangeOrder getOrderByID(Long orderID){
        return null;
    }

    public boolean widthraw(String account,String secretPhrase,BigDecimal amount,String address,String cryptocurrency){
        return false;
    }


}
