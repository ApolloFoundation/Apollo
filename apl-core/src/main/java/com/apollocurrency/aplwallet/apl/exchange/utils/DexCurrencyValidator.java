package com.apollocurrency.aplwallet.apl.exchange.utils;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;

/**
 * Examples of latest approach of working with currency pairs.
 * <p>
 * BUY | ALP-ETH
 * SEL | APL-ETH
 * <p>
 * BUY | ALP-PAX
 * SEL | APL-PAX
 */
public class DexCurrencyValidator {

    public static boolean haveFreezeOrRefundApl(DexOrder order) {
        return haveFreezeOrRefundApl(order.getType(), order.getOrderCurrency(), order.getPairCurrency());
    }


    public static boolean haveFreezeOrRefundApl(OrderType orderType, DexCurrency offerCurrencies, DexCurrency pairCurrencies) {
        if ((orderType.isSell() && offerCurrencies.isApl())) {
            return true;
        }

        return false;
    }

    public static boolean haveFreezeOrRefundEthOrPax(DexOrder order) {
        //For backward compatibility.
        if (order.getType().isSell() && order.getOrderCurrency().isEthOrPax()) {
            return true;
        }

        if (order.getType().isBuy() && order.getPairCurrency().isEthOrPax()) {
            return true;
        }

        return false;
    }

    public static void requireEthOrPaxRefundable(DexOrder offer) throws AplException.ExecutiveProcessException {
        if (!haveFreezeOrRefundEthOrPax(offer)) {
            throw new AplException.ExecutiveProcessException("Withdraw not supported for " + offer.getType() + " | " + offer.getOrderCurrency() + "-" + offer.getPairCurrency());
        }
    }

    public static void requireAplRefundable(DexOrder order) throws AplException.ExecutiveProcessException {
        if (!haveFreezeOrRefundApl(order)) {
            throw new AplException.ExecutiveProcessException("Withdraw not supported for " + order.getType() + " | " + order.getOrderCurrency() + "-" + order.getPairCurrency());
        }
    }


    public static boolean isEthOrPaxAddress(String address) {
        return address.contains("0x");
    }


}
