package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBMatchingRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DexMatchingService {

    private DexOfferDao dexOfferDao;
    private static final Logger log = LoggerFactory.getLogger(DexMatchingService.class);
    
    @Inject
    public DexMatchingService(DexOfferDao dexOfferDao) {
        this.dexOfferDao = dexOfferDao;
    }

    @Transactional
    public List<DexOffer> getOffersForMatching(DexOfferDBMatchingRequest dexOfferDBMatchingRequest, String orderby){
        return dexOfferDao.getOffersForMatchingPure(dexOfferDBMatchingRequest, orderby);
    }
}
