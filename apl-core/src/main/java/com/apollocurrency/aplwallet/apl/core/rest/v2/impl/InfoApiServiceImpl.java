package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.InfoApiService;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.model.HealthResponse;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.NonNull;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
public class InfoApiServiceImpl implements InfoApiService {

    private final DatabaseManager databaseManager;
    private final PropertiesHolder propertiesHolder;
    private final BlockChainInfoService blockChainInfoService;
    private final TransactionProcessor transactionProcessor;

    @Inject
    public InfoApiServiceImpl(@NonNull PropertiesHolder propertiesHolder,
                              @NonNull DatabaseManager databaseManager,
                              @NonNull BlockChainInfoService blockChainInfoService,
                              @NonNull TransactionProcessor transactionProcessor) {
        this.propertiesHolder = propertiesHolder;
        this.databaseManager = databaseManager;
        this.blockChainInfoService = blockChainInfoService;
        this.transactionProcessor = transactionProcessor;
    }

    public Response getHealthInfo(SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        HealthResponse response = new HealthResponse();
        response.setMaxUnconfirmedTxCount(propertiesHolder.getIntProperty("apl.maxUnconfirmedTransactions"));
        response.setUnconfirmedTxCacheSize(transactionProcessor.getWaitingTransactionsCacheSize());
        response.setBlockchainHeight(blockChainInfoService.getHeight());

        int totalConnections = -1;
        int activeConnections = -1;
        int idleConnections = -1;

        HikariPoolMXBean jmxBean = databaseManager.getDataSource().getJmxBean();
        if (jmxBean != null) {
            totalConnections = jmxBean.getTotalConnections();
            activeConnections = jmxBean.getActiveConnections();
            idleConnections = jmxBean.getIdleConnections();
        }
        response.setDbConnectionTotal(totalConnections);
        response.setDbConnectionActive(activeConnections);
        response.setDbConnectionIdle(idleConnections);
        response.setIsTrimActive(blockChainInfoService.isTrimming());

        return builder.bind(response).build();
    }
}
