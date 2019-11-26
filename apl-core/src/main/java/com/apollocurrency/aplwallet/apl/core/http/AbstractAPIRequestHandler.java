/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.core.account.service.*;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.exchange.service.DexOrderProcessor;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONStreamAware;

public abstract class AbstractAPIRequestHandler {

    private List<String> parameters;
    private String fileParameter;
    private Set<APITag> apiTags;
    private Blockchain blockchain;
    private BlockchainProcessor blockchainProcessor;
    private BlockchainConfig blockchainConfig;
    private TransactionProcessor transactionProcessor;
    protected TimeService timeService = CDI.current().select(TimeService.class).get();
    private DatabaseManager databaseManager;
    protected AdminPasswordVerifier apw =  CDI.current().select(AdminPasswordVerifier.class).get();
    protected ElGamalEncryptor elGamal = CDI.current().select(ElGamalEncryptor.class).get();
    protected PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    private AccountService accountService;
    private AccountPublicKeyService accountPublicKeyService;
    private AccountLedgerService accountLedgerService;
    private AccountAssetService accountAssetService;
    private AccountCurrencyService accountCurrencyService;
    private AccountInfoService accountInfoService;
    private AccountLeaseService accountLeaseService;
    private AccountPropertyService accountPropertyService;
    protected TrimService trimService;
    private PeersService peers;

    protected PeersService lookupPeersService() {
        if (peers == null) peers = CDI.current().select(PeersService.class).get();
        return peers;
    }

    protected AccountPropertyService lookupAccountPropertyService(){
        if (accountPropertyService == null){
            accountPropertyService = CDI.current().select(AccountPropertyServiceImpl.class).get();
        }
        return accountPropertyService;
    }

    protected AccountLeaseService lookupAccountLeaseService(){
        if (accountLeaseService == null){
            accountLeaseService = CDI.current().select(AccountLeaseServiceImpl.class).get();
        }
        return accountLeaseService;
    }

    protected AccountInfoService lookupAccountInfoService(){
        if (accountInfoService == null){
            accountInfoService = CDI.current().select(AccountInfoServiceImpl.class).get();
        }
        return accountInfoService;
    }

    protected AccountCurrencyService lookupAccountCurrencyService(){
        if (accountCurrencyService == null){
            accountCurrencyService = CDI.current().select(AccountCurrencyServiceImpl.class).get();
        }
        return accountCurrencyService;
    }

    protected AccountAssetService lookupAccountAssetService(){
        if ( accountAssetService == null){
            accountAssetService = CDI.current().select(AccountAssetServiceImpl.class).get();
        }
        return accountAssetService;
    }

    protected AccountLedgerService lookupAccountLedgerService(){
        if ( accountLedgerService == null){
            accountLedgerService = CDI.current().select(AccountLedgerServiceImpl.class).get();
        }
        return accountLedgerService;
    }

    protected AccountService lookupAccountService(){
        if ( accountService == null) {
            accountService = CDI.current().select(AccountServiceImpl.class).get();
        }
        return accountService;
    }

    protected AccountPublicKeyService lookupAccountPublickKeyService(){
        if ( accountPublicKeyService == null) {
            accountPublicKeyService = CDI.current().select(AccountPublicKeyServiceImpl.class).get();
        }
        return accountPublicKeyService;
    }

    protected Blockchain lookupBlockchain() {
        if (blockchain == null) blockchain = CDI.current().select(BlockchainImpl.class).get();
        return blockchain;
    }

    protected BlockchainConfig lookupBlockchainConfig(){
        if (blockchainConfig == null) blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        return blockchainConfig;
    }

    protected BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        return blockchainProcessor;
    }

    protected TransactionProcessor lookupTransactionProcessor() {
        if (transactionProcessor == null) transactionProcessor = CDI.current().select(TransactionProcessorImpl.class).get();
        return transactionProcessor;
    }

    protected TransactionalDataSource lookupDataSource() {
        if (databaseManager == null) {
            databaseManager = CDI.current().select(DatabaseManager.class).get();
        }
        return databaseManager.getDataSource();
    }

    protected TrimService lookupTrimService() {
        if (trimService == null) trimService = CDI.current().select(TrimService.class).get();
        return trimService;
    }

    public AbstractAPIRequestHandler(APITag[] apiTags, String... parameters) {
        this(null, apiTags, parameters);
    }

    public AbstractAPIRequestHandler(String fileParameter, APITag[] apiTags, String... origParameters) {
        List<String> parameters = new ArrayList<>();
        Collections.addAll(parameters, origParameters);
        if ((requirePassword() || parameters.contains("lastIndex")) && ! apw.disableAdminPassword) {
            parameters.add("adminPassword");
        }
        if (allowRequiredBlockParameters()) {
            parameters.add("requireBlock");
            parameters.add("requireLastBlock");
        }
        String vaultAccountParameterName = vaultAccountName();
        if (vaultAccountParameterName != null && !vaultAccountParameterName.isEmpty()) {
            parameters.add(vaultAccountParameterName);
            parameters.add("passphrase");
        }
        if (is2FAProtected()) {
            parameters.add("code2FA");
        }
       
        this.parameters = Collections.unmodifiableList(parameters);
        this.apiTags = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(apiTags)));
        this.fileParameter = fileParameter;
    }

    public final List<String> getParameters() {
        return parameters;
    }

    public final Set<APITag> getAPITags() {
        return apiTags;
    }

    public final String getFileParameter() {
        return fileParameter;
    }

    public abstract JSONStreamAware processRequest(HttpServletRequest request) throws AplException;

    public JSONStreamAware processRequest(HttpServletRequest request, HttpServletResponse response) throws AplException {
        return processRequest(request);
    }

    protected boolean requirePost() {
        return false;
    }

//    protected boolean startDbTransaction() {
//        return false;
//    }

    protected boolean requirePassword() {
        return false;
    }

    protected boolean allowRequiredBlockParameters() {
        return true;
    }

    protected boolean requireBlockchain() {
        return true;
    }

    protected boolean requireFullClient() {
        return false;
    }

    protected boolean logRequestTime() { return false; }

    protected boolean is2FAProtected() {
        return false;
    }

    protected String vaultAccountName() {
        return null;
    }

}
