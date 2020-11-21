package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderTable;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderFreezing;
import com.apollocurrency.aplwallet.apl.exchange.service.DexSmartContractService;
import com.google.common.cache.CacheLoader;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DexOrderFreezingCacheLoaderProducer {
    private DexOrderTable table;
    private DexSmartContractService smartContractService;

    @Inject
    public DexOrderFreezingCacheLoaderProducer(DexOrderTable table, DexSmartContractService smartContractService) {
        this.table = table;
        this.smartContractService = smartContractService;
    }

    @Produces
    public CacheLoader<Long, OrderFreezing> getDexFreezingLoader() {
        return CacheLoader.from((id) -> {
            DexOrder order = table.getByTxId(id);
            if (order == null) {
                throw new RuntimeException("Order does not exist " + id);
            }
            return new OrderFreezing(id, smartContractService.hasFrozenMoney(order));
        });
    }
}
