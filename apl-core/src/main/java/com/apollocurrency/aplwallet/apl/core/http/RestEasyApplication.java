/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.api.v2.AccountApi;
import com.apollocurrency.aplwallet.api.v2.CurrenciesApi;
import com.apollocurrency.aplwallet.api.v2.InfoApi;
import com.apollocurrency.aplwallet.api.v2.OperationApi;
import com.apollocurrency.aplwallet.api.v2.StateApi;
import com.apollocurrency.aplwallet.api.v2.TransactionApi;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.AccountControlController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.AccountController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.BlockController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.DebugController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.DexController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.DexTransactionSendingController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.KeyStoreController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.MandatoryTransactionController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.Metadata;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.NetworkController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.NodeControlController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.NodeInfoController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.ServerInfoController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.ShardController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.TradingDataController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.TransportInteractionController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.UnconfirmedTransactionController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.UpdateController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.UserErrorMessageController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.UtilsController;
import com.apollocurrency.aplwallet.apl.exchange.service.DexMatcherServiceImpl;
import io.firstbridge.kms.infrastructure.web.resource.AuthorizationController;
import io.firstbridge.kms.infrastructure.web.resource.KmsRegistrationController;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * REST and Swagger configuration and fire-up
 *
 * @author alukin@gmail.com
 */
@ApplicationPath("/rest")
public class RestEasyApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {

        HashSet<Class<?>> set = new HashSet<>();
        set.add(Metadata.class);
        set.add(NodeInfoController.class);
        set.add(KeyStoreController.class);
        set.add(NetworkController.class);
        set.add(DebugController.class);
        // Transfer Eth, doesn't use yet.
//        set.add(WalletEthController.class);
        set.add(DexController.class);
        set.add(DexMatcherServiceImpl.class);
        set.add(NodeControlController.class);
        set.add(TransportInteractionController.class);
        set.add(ShardController.class);
        set.add(AccountController.class);
        set.add(MandatoryTransactionController.class);
        set.add(UserErrorMessageController.class);
        set.add(TradingDataController.class);
        set.add(DexTransactionSendingController.class);
        set.add(UtilsController.class);
        set.add(ServerInfoController.class);
        set.add(UpdateController.class);
        set.add(BlockController.class);
        set.add(AccountControlController.class);
        set.add(UnconfirmedTransactionController.class);
        // KMS part
        set.add(AuthorizationController.class);
        set.add(KmsRegistrationController.class);

        //API V2
        set.add(AccountApi.class);
        set.add(InfoApi.class);
        set.add(OperationApi.class);
        set.add(StateApi.class);
        set.add(TransactionApi.class);
        set.add(CurrenciesApi.class);
        //API V2 Services

        //swagger openapi endpoint
        set.add(OpenApiResource.class);
        return set;
    }
}
