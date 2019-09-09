package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class DexMatchingService {

    private DexOrderDao dexOrderDao;
    private static final Logger log = LoggerFactory.getLogger(DexMatchingService.class);
    
    @Inject
    public DexMatchingService(DexOrderDao dexOrderDao) {
        this.dexOrderDao = dexOrderDao;
    }

    @Transactional
    public List<DexOrder> getOffersForMatching(DexOfferDBMatchingRequest dexOfferDBMatchingRequest, String orderby) {
        return dexOrderDao.getOffersForMatchingPure(dexOfferDBMatchingRequest, orderby);
    }
}
