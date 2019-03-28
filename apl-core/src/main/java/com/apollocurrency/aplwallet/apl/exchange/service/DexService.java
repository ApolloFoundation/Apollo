package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountService;
import com.apollocurrency.aplwallet.apl.exchange.model.Balances;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeOrder;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DexService {

    private AccountService accountService = CDI.current().select(AccountService.class).get();

    public Balances getBalances(Account account, String ethAddress, String paxAddress){
        Balances balances = accountService.getAccountBalances(account, true, ethAddress, paxAddress);
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
