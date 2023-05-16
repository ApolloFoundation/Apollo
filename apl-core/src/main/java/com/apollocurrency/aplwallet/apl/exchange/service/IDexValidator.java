/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;


/**
 * @author Serhiy Lymar
 */
public interface IDexValidator {


    /**
     * currency-specific validation (Ethereum)
     *
     * @param DexOffer myOffer - created offer to validate
     * @param DexOffer hisOffer - matched offer
     * @return 1 if success, -1 if
     */
    int validateOfferBuyAplEth(DexOrder myOffer, DexOrder hisOffer);

    /**
     * currency-specific validation Phasing (Ethereum)
     *
     * @param DexOffer myOffer - created offer to validate
     * @param DexOffer hisOffer - matched offer
     * @return 1 if success, -1 if
     */
    int validateOfferBuyAplEthPhasing(DexOrder myOffer, DexOrder hisOffer, Long txId);

    /**
     * currency-specific validation for active deposit (Ethereum)
     *
     * @param DexOffer myOffer - created offer to validate
     * @param DexOffer hisOffer - matched offer
     * @return 1 if success, -1 if
     */
    int validateOfferSellAplEthActiveDeposit(DexOrder myOffer, DexOrder hisOffer);

    /**
     * currency-specific validation for atomic swap (Ethereum)
     *
     * @param DexOffer myOffer - created offer to validate
     * @param DexOffer hisOffer - matched offer
     * @return 1 if success, -1 if
     */
    int validateOfferSellAplEthAtomicSwap(DexOrder myOffer, DexOrder hisOffer, byte[] secretHash);

    /**
     * currency-specific validation (Pax)
     *
     * @param DexOffer myOffer - created offer to validate
     * @param DexOffer hisOffer - matched offer
     */
    int validateOfferBuyAplPax(DexOrder myOffer, DexOrder hisOffer);

    /**
     * currency-specific validation for active deposit (Pax)
     *
     * @param DexOffer myOffer - created offer to validate
     * @param DexOffer hisOffer - matched offer
     */
    int validateOfferSellAplPaxActiveDeposit(DexOrder myOffer, DexOrder hisOffer);


    /**
     * currency-specific validation for atomic swap (Pax)
     *
     * @param DexOffer myOffer - created offer to validate
     * @param DexOffer hisOffer - matched offer
     */
    int validateOfferSellAplPaxAtomicSwap(DexOrder myOffer, DexOrder hisOffer, byte[] secretHash);

}
