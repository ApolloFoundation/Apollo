/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.response.UnconfirmedTransactionCountResponse;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import io.swagger.v3.oas.annotations.Operation;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/txs")
public class UnconfirmedTransactionController {
    private TransactionProcessor processor;

    @Inject
    public UnconfirmedTransactionController(TransactionProcessor processor) {
        this.processor = processor;
    }

    public UnconfirmedTransactionController() {
    }

    @GET
    @Path("/unconfirmed-count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"mempool"}, summary = "Unconfirmed transaction quantity",
        description = "Return current number of unconfirmed transsactions in waiting and processed states  ")
    @PermitAll
    public Response getUnconfirmedTransactionCount() {
        return Response.ok(new UnconfirmedTransactionCountResponse(processor.getUnconfirmedTxCount(), processor.getWaitingTransactionsCacheSize())).build();
    }
}
