package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBMatchingRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class DexMatchingService {

    private DexOrderDao dexOrderDao;

    @Inject
    public DexMatchingService(DexOrderDao dexOrderDao) {
        this.dexOrderDao = dexOrderDao;
    }

    @Transactional
    public List<DexOrder> getOffersForMatching(DexOrderDBMatchingRequest dexOrderDBMatchingRequest, String orderby) {
        return dexOrderDao.getOffersForMatchingPure(dexOrderDBMatchingRequest, orderby);
    }
}
