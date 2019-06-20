/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Setter;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Apollo accounts endpoint
 */

@Path("/accounts")
public class AccountController {

    @Inject @Setter
    Blockchain blockchain;

    @Inject @Setter
    private AccountService service;

    @Inject @Setter
    private AccountConverter converter;

    @Path("/account")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns account information",
            description = "Returns account information by account id.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountDTO.class)))
            })
    public Response getAccount(
            @Parameter(description = "The certain account ID.", required = true) @QueryParam("account") String accountStr,
            @Parameter(description = "include additional lessors information.") @QueryParam("includeLessors") @DefaultValue("false") Boolean includeLessors,
            @Parameter(description = "include additional assets information.") @QueryParam("includeAssets") @DefaultValue("false") Boolean includeAssets,
            @Parameter(description = "include additional currency information.") @QueryParam("includeCurrencies") @DefaultValue("false") Boolean includeCurrencies,
            @Parameter(description = "include effective balance.") @QueryParam("includeEffectiveBalance") @DefaultValue("false") Boolean includeEffectiveBalance,
            @Parameter(description = "require block.") @QueryParam("requireBlock") String requireBlockStr,
            @Parameter(description = "require last block.") @QueryParam("requireLastBlock") String requireLastBlockStr
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        if (accountStr == null) {
            return response.error( ApiErrors.MISSING_PARAM, "account").build();
        }

        Account account  = service.getAccount(accountStr);

        if (account == null) {
            return response.error( ApiErrors.UNKNOWN_VALUE, "account", accountStr).build();
        }

        AccountDTO dto = converter.convert(account);
        if (includeEffectiveBalance){
            converter.addEffectiveBalances(dto, account);
        }
        if (includeLessors){
            List<Account> lessors = service.getLessors(account);
            converter.addAccountLessors(dto, lessors, includeEffectiveBalance);
        }
        if(includeAssets){
            List<AccountAsset> assets = service.getAccountAssets(account);
            converter.addAccountAssets(dto, assets);
        }
        if(includeCurrencies){
            List<AccountCurrency> currencies = service.getAccountCurrencies(account);
            converter.addAccountCurrencies(dto, currencies);
        }

        return response.bind(dto).build();
    }

}
