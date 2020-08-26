package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.InfoApiService;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.model.BlockchainInfo;
import com.apollocurrency.aplwallet.api.v2.model.HealthResponse;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
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
    private final BlockchainConfig blockchainConfig;
    private final BlockChainInfoService blockChainInfoService;
    private final TransactionProcessor transactionProcessor;
    private final AccountService accountService;

    @Inject
    public InfoApiServiceImpl(@NonNull PropertiesHolder propertiesHolder,
                              @NonNull DatabaseManager databaseManager,
                              @NonNull BlockchainConfig blockchainConfig,
                              @NonNull BlockChainInfoService blockChainInfoService,
                              @NonNull TransactionProcessor transactionProcessor,
                              @NonNull AccountService accountService) {
        this.propertiesHolder = propertiesHolder;
        this.databaseManager = databaseManager;
        this.blockchainConfig = blockchainConfig;
        this.blockChainInfoService = blockChainInfoService;
        this.transactionProcessor = transactionProcessor;
        this.accountService = accountService;
    }

    @Override
    public Response getBlockchainInfo(SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        BlockchainInfo blockchainState = new BlockchainInfo();
        blockchainState.setChainid(blockchainConfig.getChain().getChainId().toString());
        blockchainState.setGenesisAccount(Long.toUnsignedString(GenesisImporter.CREATOR_ID));
        blockchainState.setTicker(blockchainConfig.getCoinSymbol());
        blockchainState.setConsensus("POS");
        blockchainState.setMining("Pre-mining");
        Account account = accountService.getAccount(GenesisImporter.CREATOR_ID);
        blockchainState.setTotalSupply(blockchainConfig.getInitialSupply());
        blockchainState.setBurning(
            blockchainConfig.getInitialSupply() - Math.abs(account.getBalanceATM()) / blockchainConfig.getOneAPL()
        );

        return builder.bind(blockchainState).build();
    }

    @Override
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
