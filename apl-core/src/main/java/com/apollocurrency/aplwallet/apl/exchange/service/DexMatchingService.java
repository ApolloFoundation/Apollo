package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexOrderDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
