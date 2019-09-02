package com.apollocurrency.aplwallet.apl.exchange.utils;

import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import com.apollocurrency.aplwallet.apl.util.AplException;

/**
 * Examples of latest approach of working with currency pairs.
 *
 * BUY | ALP-ETH
 * SEL | APL-ETH
 *
 * BUY | ALP-PAX
 * SEL | APL-PAX
 */
public class DexCurrencyValidator {

    public static boolean haveFreezeOrRefundApl(DexOffer offer){
        return haveFreezeOrRefundApl(offer.getType(), offer.getOfferCurrency(), offer.getPairCurrency());
    }


    public static boolean haveFreezeOrRefundApl(OfferType offerType, DexCurrencies offerCurrencies, DexCurrencies pairCurrencies){
         if((offerType.isSell() && offerCurrencies.isApl())){
            return true;
         }

         return false;
    }

    public static boolean haveFreezeOrRefundEthOrPax(DexOffer offer){
        //For backward compatibility.
        if(offer.getType().isSell() && offer.getOfferCurrency().isEthOrPax()){
            return true;
        }

        if(offer.getType().isBuy() && offer.getPairCurrency().isEthOrPax()){
            return true;
        }

        return false;
    }

    public static void checkHaveFreezeOrRefundEthOrPax(DexOffer offer) throws AplException.ExecutiveProcessException {
        if(!haveFreezeOrRefundEthOrPax(offer)){
            throw new AplException.ExecutiveProcessException("Withdraw not supported for " + offer.getType() +" | "+ offer.getOfferCurrency() + "-" + offer.getPairCurrency());
        }
    }

    public static void checkHaveFreezeOrRefundApl(DexOffer offer) throws AplException.ExecutiveProcessException {
        if(!haveFreezeOrRefundApl(offer)){
            throw new AplException.ExecutiveProcessException("Withdraw not supported for " + offer.getType() +" | "+ offer.getOfferCurrency() + "-" + offer.getPairCurrency());
        }
    }


    public static boolean isEthOrPaxAddress(String address) {
        return address.contains("0x");
    }



}
