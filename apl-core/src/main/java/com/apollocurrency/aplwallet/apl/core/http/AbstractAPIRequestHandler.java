/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Vetoed
public abstract class AbstractAPIRequestHandler {

    private List<String> parameters;
    private String fileParameter;
    private Set<APITag> apiTags;
    private Blockchain blockchain;
    private BlockchainProcessor blockchainProcessor;
    private TransactionProcessor transactionProcessor;
    protected static volatile TimeService timeService = CDI.current().select(TimeService.class).get();
    private DatabaseManager databaseManager;
    protected  static AdminPasswordVerifier apw =  CDI.current().select(AdminPasswordVerifier.class).get();
    protected ElGamalEncryptor elGamal = CDI.current().select(ElGamalEncryptor.class).get();
    protected static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    private PeersService peers;

    protected PeersService lookupPeersService() {
        if (peers == null) peers = CDI.current().select(PeersService.class).get();
        return peers;
    }

    protected Blockchain lookupBlockchain() {
        if (blockchain == null) blockchain = CDI.current().select(BlockchainImpl.class).get();
        return blockchain;
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
