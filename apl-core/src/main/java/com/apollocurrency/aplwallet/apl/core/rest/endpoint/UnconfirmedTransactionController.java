/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.response.UnconfirmedTransactionCountResponse;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/txs")
public class UnconfirmedTransactionController {
    private MemPool memPool;

    @Inject
    public UnconfirmedTransactionController(MemPool memPool) {
        this.memPool = memPool;
    }

    public UnconfirmedTransactionController() {
    }

    /**
     *  Use  com.apollocurrency.aplwallet.apl.core.rest.v2.impl.InfoApiServiceImpl#getHealthInfo(jakarta.ws.rs.core.SecurityContext)
     */
    @Deprecated
    @GET
    @Path("/unconfirmed-count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"mempool"}, summary = "Unconfirmed transaction quantity",
        description = "Return current number of unconfirmed transsactions  ")
    @PermitAll
    public Response getUnconfirmedTransactionCount() {
        return Response.ok(new UnconfirmedTransactionCountResponse(memPool.getCount(), memPool.processingQueueSize())).build();
    }
}
