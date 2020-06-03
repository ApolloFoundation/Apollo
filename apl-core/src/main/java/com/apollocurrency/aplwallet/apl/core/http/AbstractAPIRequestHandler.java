/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountInfoService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountInfoServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountLeaseService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountLeaseServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountLedgerService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountLedgerServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPropertyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPropertyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.alias.service.AliasService;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dgs.DGSService;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.monetary.service.AssetService;
import com.apollocurrency.aplwallet.apl.core.monetary.service.AssetTransferService;
import com.apollocurrency.aplwallet.apl.core.order.entity.AskOrder;
import com.apollocurrency.aplwallet.apl.core.order.entity.BidOrder;
import com.apollocurrency.aplwallet.apl.core.order.service.OrderService;
import com.apollocurrency.aplwallet.apl.core.order.service.impl.AskOrderServiceImpl;
import com.apollocurrency.aplwallet.apl.core.order.service.impl.BidOrderServiceImpl;
import com.apollocurrency.aplwallet.apl.core.order.service.qualifier.AskOrderService;
import com.apollocurrency.aplwallet.apl.core.order.service.qualifier.BidOrderService;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.tagged.TaggedDataService;
import com.apollocurrency.aplwallet.apl.core.trade.service.TradeService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;
import com.apollocurrency.aplwallet.apl.util.UPnP;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractAPIRequestHandler {

    protected BlockchainProcessor blockchainProcessor;
    protected TimeService timeService = CDI.current().select(TimeService.class).get();
    protected AdminPasswordVerifier apw = CDI.current().select(AdminPasswordVerifier.class).get();
    protected ElGamalEncryptor elGamal = CDI.current().select(ElGamalEncryptor.class).get();
    protected PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    protected final AliasService aliasService = CDI.current().select(AliasService.class).get();
    protected final OrderService<AskOrder, ColoredCoinsAskOrderPlacement> askOrderService =
        CDI.current().select(AskOrderServiceImpl.class, AskOrderService.Literal.INSTANCE).get();
    protected final OrderService<BidOrder, ColoredCoinsBidOrderPlacement> bidOrderService =
        CDI.current().select(BidOrderServiceImpl.class, BidOrderService.Literal.INSTANCE).get();
    protected final TradeService tradeService = CDI.current().select(TradeService.class).get();
    protected UPnP upnp = CDI.current().select(UPnP.class).get();
    protected DGSService service = CDI.current().select(DGSService.class).get();
    protected TaggedDataService taggedDataService = CDI.current().select(TaggedDataService.class).get();
    protected PrunableMessageService prunableMessageService = CDI.current().select(PrunableMessageService.class).get();
    protected AssetService assetService = CDI.current().select(AssetService.class).get();

    protected TrimService trimService;
    private List<String> parameters;
    private String fileParameter;
    private Set<APITag> apiTags;
    private Blockchain blockchain;
    private BlockchainConfig blockchainConfig;
    private TransactionProcessor transactionProcessor;
    private DatabaseManager databaseManager;
    private AccountService accountService;
    private AccountPublicKeyService accountPublicKeyService;
    private AccountLedgerService accountLedgerService;
    private AccountAssetService accountAssetService;
    private AccountCurrencyService accountCurrencyService;
    private AccountInfoService accountInfoService;
    private AccountLeaseService accountLeaseService;
    private AccountPropertyService accountPropertyService;
    private PeersService peers;
    private AccountControlPhasingService accountControlPhasingService;
    private AssetTransferService assetTransferService;

    public AbstractAPIRequestHandler(APITag[] apiTags, String... parameters) {
        this(null, apiTags, parameters);
    }

    public AbstractAPIRequestHandler(String fileParameter, APITag[] apiTags, String... origParameters) {
        List<String> parameters = new ArrayList<>();
        Collections.addAll(parameters, origParameters);
        if ((requirePassword() || parameters.contains("lastIndex")) && !apw.isDisabledAdminPassword()) {
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

    protected PeersService lookupPeersService() {
        if (peers == null) peers = CDI.current().select(PeersService.class).get();
        return peers;
    }

    protected AccountPropertyService lookupAccountPropertyService() {
        if (accountPropertyService == null) {
            accountPropertyService = CDI.current().select(AccountPropertyServiceImpl.class).get();
        }
        return accountPropertyService;
    }

    protected AccountLeaseService lookupAccountLeaseService() {
        if (accountLeaseService == null) {
            accountLeaseService = CDI.current().select(AccountLeaseServiceImpl.class).get();
        }
        return accountLeaseService;
    }

    protected AccountInfoService lookupAccountInfoService() {
        if (accountInfoService == null) {
            accountInfoService = CDI.current().select(AccountInfoServiceImpl.class).get();
        }
        return accountInfoService;
    }

    protected AccountCurrencyService lookupAccountCurrencyService() {
        if (accountCurrencyService == null) {
            accountCurrencyService = CDI.current().select(AccountCurrencyServiceImpl.class).get();
        }
        return accountCurrencyService;
    }

    protected AccountAssetService lookupAccountAssetService() {
        if (accountAssetService == null) {
            accountAssetService = CDI.current().select(AccountAssetServiceImpl.class).get();
        }
        return accountAssetService;
    }

    protected AccountLedgerService lookupAccountLedgerService() {
        if (accountLedgerService == null) {
            accountLedgerService = CDI.current().select(AccountLedgerServiceImpl.class).get();
        }
        return accountLedgerService;
    }

    protected AccountService lookupAccountService() {
        if (accountService == null) {
            accountService = CDI.current().select(AccountServiceImpl.class).get();
        }
        return accountService;
    }

    protected AccountPublicKeyService lookupAccountPublickKeyService() {
        if (accountPublicKeyService == null) {
            accountPublicKeyService = CDI.current().select(AccountPublicKeyServiceImpl.class).get();
        }
        return accountPublicKeyService;
    }

    protected Blockchain lookupBlockchain() {
        if (blockchain == null) blockchain = CDI.current().select(BlockchainImpl.class).get();
        return blockchain;
    }

    protected BlockchainConfig lookupBlockchainConfig() {
        if (blockchainConfig == null) blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        return blockchainConfig;
    }

    protected BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null)
            blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        return blockchainProcessor;
    }

    protected TransactionProcessor lookupTransactionProcessor() {
        if (transactionProcessor == null)
            transactionProcessor = CDI.current().select(TransactionProcessorImpl.class).get();
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

    public AccountControlPhasingService lookupAccountControlPhasingService() {
        if (accountControlPhasingService == null) {
            accountControlPhasingService = CDI.current().select(AccountControlPhasingService.class).get();
        }
        return accountControlPhasingService;
    }

    public AssetTransferService lookupAssetTransferService() {
        if (assetTransferService == null) {
            assetTransferService = CDI.current().select(AssetTransferService.class).get();
        }
        return assetTransferService;
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

    protected boolean logRequestTime() {
        return false;
    }

    protected boolean is2FAProtected() {
        return false;
    }

    protected String vaultAccountName() {
        return null;
    }

}
