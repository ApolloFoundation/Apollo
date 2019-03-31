/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.core.rest.endpoint.BackendControlEndpoint;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.DexController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.KeyStoreController;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.Metadata;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.ServerInfoEndpoint;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * REST and Swagger configuration and fire-up
 * @author alukin@gmail.com
 */
@ApplicationPath("/rest")
public class RestEasyApplication extends Application  {

    @Override
    public Set<Class<?>> getClasses() {

        HashSet<Class<?>> set = new HashSet<>();
        set.add(Metadata.class);
        set.add(ServerInfoEndpoint.class);
        set.add(KeyStoreController.class);
        // Transfer Eth, doesn't use yet.
//        set.add(WalletEthController.class);
        set.add(DexController.class);
        set.add(BackendControlEndpoint.class);


        //TODO: add all endpoints below
        //swagger openapi endpoint
        set.add(OpenApiResource.class);
        return set;
    }
}
