package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBMatchingRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class DexMatchingService {

    private DexOfferDao dexOfferDao;

    @Inject
    public DexMatchingService(DexOfferDao dexOfferDao) {
        this.dexOfferDao = dexOfferDao;
    }

    @Transactional
    public List<DexOffer> getOffersForMatching(DexOfferDBMatchingRequest dexOfferDBMatchingRequest){
        return dexOfferDao.getOffersForMatching(dexOfferDBMatchingRequest);
    }
}
