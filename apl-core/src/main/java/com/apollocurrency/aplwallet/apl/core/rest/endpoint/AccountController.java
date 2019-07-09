/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.Account2FADTO;
import com.apollocurrency.aplwallet.api.dto.Account2FADetailsDTO;
import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.WalletKeysInfoDTO;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.http.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FAConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FADetailsConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountBalanceService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;
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
    private Account2FAHelper account2FAHelper;

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

    @Inject @Setter
    private Account2FADetailsConverter faDetailsConverter;

    @Inject @Setter
    private Account2FAConverter faConverter;

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

        Account account  = accountBalanceService.retrieveAccountByAccountId(accountStr);

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
            List<AccountAsset> assets = accountAssetService.getAssetAccounts(account, 0, -1);
            converter.addAccountAssets(dto, assets);
        }
        if(includeCurrencies){
            List<AccountCurrency> currencies = accountCurrencyService.getCurrencies(account);
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
        WalletKeysInfo walletKeysInfo = account2FAHelper.generateUserWallet(passphrase);

        if (walletKeysInfo == null){
            return response.error( ApiErrors.ACCOUNT_GENERATION_ERROR).build();
        }

        WalletKeysInfoDTO dto = walletKeysConverter.convert(walletKeysInfo);

        return response.bind(dto).build();
    }

    @Path("/confirm2FA")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Confirm two factor authentication",
            description = "Confirm two factor authentication.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountDTO.class)))
            })
    public Response confirm2FA(
            @Parameter(description = "The passphrase") @FormParam("passphrase") String passphraseParam,
            @Parameter(description = "The secret phrase") @FormParam("secretPhrase") String secretPhraseParam,
            @Parameter(description = "The certain account ID.") @FormParam("account") String accountStr,
            @Parameter(description = "The 2FA code.", required = true) @FormParam("code2FA") Integer code2FA
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        TwoFactorAuthParameters params2FA = account2FAHelper.verify2FA(accountStr, passphraseParam, secretPhraseParam, code2FA);

        account2FAHelper.confirm2FA(params2FA, code2FA);
        Account2FADTO dto = faConverter.convert(params2FA);

        return response.bind(dto).build();
    }


    @Path("/disable2FA")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Disable two factor authentication",
            description = "Disable two factor authentication.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Account2FADetailsDTO.class)))
            })
    public Response disable2FA(
            @Parameter(description = "The passphrase") @FormParam("passphrase") String passphraseParam,
            @Parameter(description = "The secret phrase") @FormParam("secretPhrase") String secretPhraseParam,
            @Parameter(description = "The certain account ID.") @FormParam("account") String accountStr,
            @Parameter(description = "The 2FA code.", required = true) @FormParam("code2FA") Integer code2FA
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        TwoFactorAuthParameters params2FA = account2FAHelper.verify2FA(accountStr, passphraseParam, secretPhraseParam, code2FA);

        account2FAHelper.disable2FA(params2FA, code2FA);

        Account2FADTO dto = faConverter.convert(params2FA);

        return response.bind(dto).build();
    }

    @Path("/enable2FA")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Enable two factor authentication",
            description = "Enable two factor authentication.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Account2FADetailsDTO.class)))
            })
    public Response enable2FA(
            @Parameter(description = "The passphrase") @FormParam("passphrase") String passphraseParam,
            @Parameter(description = "The secret phrase") @FormParam("secretPhrase") String secretPhraseParam,
            @Parameter(description = "The certain account ID.") @FormParam("account") String accountStr
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        TwoFactorAuthParameters params2FA = account2FAHelper.parse2FARequestParams(accountStr, passphraseParam, secretPhraseParam);

        TwoFactorAuthDetails twoFactorAuthDetails = account2FAHelper.enable2FA(params2FA);

        Account2FADetailsDTO dto = faDetailsConverter.convert(twoFactorAuthDetails);
        faDetailsConverter.addAccount(dto, params2FA.getAccountId());

        return response.bind(dto).build();
    }

}
