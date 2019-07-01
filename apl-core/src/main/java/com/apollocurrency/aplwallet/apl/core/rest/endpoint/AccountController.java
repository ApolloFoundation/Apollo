/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.WalletKeysInfoDTO;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountBalanceService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

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
    private AccountService accountService;

    @Inject @Setter
    private AccountAssetService accountAssetService;

    @Inject @Setter
    private AccountCurrencyService accountCurrencyService;

    @Inject @Setter
    private AccountBalanceService accountBalanceService;

    @Inject @Setter
    private AccountConverter converter;

    @Inject @Setter
    private WalletKeysConverter walletKeysConverter;

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

        Account account  = accountBalanceService.getAccount(accountStr);

        if (account == null) {
            return response.error( ApiErrors.UNKNOWN_VALUE, "account", accountStr).build();
        }

        AccountDTO dto = converter.convert(account);
        if (includeEffectiveBalance){
            converter.addEffectiveBalances(dto, account);
        }
        if (includeLessors){
            List<Account> lessors = accountService.getLessors(account);
            converter.addAccountLessors(dto, lessors, includeEffectiveBalance);
        }
        if(includeAssets){
            List<AccountAsset> assets = accountAssetService.getAssetAccounts(account);
            converter.addAccountAssets(dto, assets);
        }
        if(includeCurrencies){
            List<AccountCurrency> currencies = accountCurrencyService.getCurrencyAccounts(account);
            converter.addAccountCurrencies(dto, currencies);
        }

        return response.bind(dto).build();
    }

    @Path("/account")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Generate account Returns account information",
            description = "Returns new account, publicKey, accountRS.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountDTO.class)))
            })
    public Response generateAccount(
            @Parameter(description = "The passphrase", required = true) @FormParam("passphrase") String passphrase,
            @Parameter(description = "require block.") @FormParam("requireBlock") String requireBlockStr,
            @Parameter(description = "require last block.") @FormParam("requireLastBlock") String requireLastBlockStr
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        if (StringUtils.isEmpty(passphrase)){
            return response.error( ApiErrors.MISSING_PARAM, "passphrase").build();
        }
        WalletKeysInfo walletKeysInfo = null;
        try {
            walletKeysInfo = Helper2FA.generateUserWallet(passphrase);
        } catch (ParameterException ignored) {}

        if (walletKeysInfo == null){
            return response.error( ApiErrors.ACCOUNT_GENERATION_ERROR).build();
        }

        WalletKeysInfoDTO dto = walletKeysConverter.convert(walletKeysInfo);

        return response.bind(dto).build();
    }

}
