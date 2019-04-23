package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeBalances;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeOrder;
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

    @Inject
    public DexService(EthereumWalletService ethereumWalletService, DexOfferDao dexOfferDao) {
        this.ethereumWalletService = ethereumWalletService;
        this.dexOfferDao = dexOfferDao;
    }

    @Transactional
    public void updateOffer (DexOffer offer){
        dexOfferDao.save(offer);
    }

    @Transactional
    public DexOffer getOfferByTransactionId(Long transactionId){
        return dexOfferDao.getByTransactionId(transactionId);
    }

    @Transactional
    public void deleteOfferByTransactionId(Long transactionId){
        dexOfferDao.deleteByTransactionId(transactionId);
    }

    @Transactional
    public void saveOffer (DexOffer offer){
        if(dexOfferDao.getByTransactionId(offer.getTransactionId()) == null){
            dexOfferDao.save(offer);
        }
    }

    @Transactional
    public List<DexOffer> getOffers(DexOfferDBRequest dexOfferDBRequest){
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
