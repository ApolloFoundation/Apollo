/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.account.Account2FADTO;
import com.apollocurrency.aplwallet.api.dto.account.Account2FADetailsDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountKeyDTO;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.model.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FAConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FADetailsConverter;
import com.apollocurrency.aplwallet.apl.core.rest.filters.Secured2FA;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.AccountIdParameter;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.core.rest.utils.RestParametersParser;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Two factor auth endpoint (2FA)
 */
@OpenAPIDefinition(info = @Info(description = "Provide methods to operate with accounts"))
@SecurityScheme(type = SecuritySchemeType.APIKEY, name = "admin_api_key", in = SecuritySchemeIn.QUERY, paramName = "adminPassword")
@NoArgsConstructor
@Slf4j
@Path("/2fa")
@Setter
public class TwoFactorManageController {

    private Account2FAHelper account2FAHelper;

    private Account2FADetailsConverter faDetailsConverter;

    private Account2FAConverter faConverter;

    @Inject
    public TwoFactorManageController(Account2FAHelper account2FAHelper,
                                     Account2FADetailsConverter faDetailsConverter,
                                     Account2FAConverter faConverter) {

        this.account2FAHelper = account2FAHelper;
        this.faDetailsConverter = faDetailsConverter;
        this.faConverter = faConverter;
    }

    @Path("/export-key")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "Export the public key associated with an account ID.",
        description = "Export the public key associated with an account ID.",
        tags = {"accounts"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "text/html",
                    schema = @Schema(implementation = AccountKeyDTO.class)))
        })
    @PermitAll
    //TODO: It's a good idea to protect the exportkey method by @Secured2FA annotation
    public Response exportKey(@Parameter(description = "The secret passphrase of the account.", required = true)
                              @FormParam("passphrase") @NotNull String passphrase,
                              @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
                              @FormParam("account") @NotNull AccountIdParameter accountIdParameter
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        accountIdParameter.get();
        TwoFactorAuthParameters params2FA = account2FAHelper.create2FAParameters(
            accountIdParameter.getRawData(), passphrase, null, null);

        byte[] secretBytes = account2FAHelper.findAplSecretBytes(params2FA);

        AccountKeyDTO dto = new AccountKeyDTO(
            Long.toUnsignedString(params2FA.getAccountId()),
            Convert2.rsAccount(params2FA.getAccountId()),
            null, Convert.toHexString(secretBytes));

        return response.bind(dto).build();
    }

    @Path("/delete-key")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "Delete account",
        description = "Delete account and Remove secret bytes from keystore.",
        tags = {"accounts"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "text/html",
                    schema = @Schema(implementation = AccountKeyDTO.class)))
        })
    @PermitAll
    @Secured2FA
    public Response deleteKey(@Parameter(description = "The passphrase", required = true)
                              @FormParam("passphrase") @NotNull String passphrase,
                              @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
                              @FormParam("account") @NotNull AccountIdParameter accountIdParameter,
                              @Parameter(description = "The 2FA code.", required = true)
                              @FormParam("code2FA") Integer code2FA,
                              @Context org.jboss.resteasy.spi.HttpRequest request
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        TwoFactorAuthParameters params2FA = RestParametersParser.get2FARequestAttribute(request);

        KeyStoreService.Status status = account2FAHelper.deleteAccount(params2FA);

        AccountKeyDTO dto = new AccountKeyDTO(Long.toUnsignedString(params2FA.getAccountId()),
            Convert2.rsAccount(params2FA.getAccountId()),
            status.message, null);

        return response.bind(dto).build();
    }

    @Path("/confirm")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "Confirm two factor authentication",
        description = "Confirm two factor authentication.",
        tags = {"accounts"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "text/html",
                    schema = @Schema(implementation = Account2FADTO.class)))
        })

    @PermitAll
    @Secured2FA
    public Response confirm2FA(
        @Parameter(description = "The passphrase") @FormParam("passphrase") String passphraseParam,
        @Parameter(description = "The secret phrase") @FormParam("secretPhrase") String secretPhraseParam,
        @Parameter(description = "The account ID.") @FormParam("account") String accountStr,
        @Parameter(description = "The 2FA code.", required = true) @FormParam("code2FA") @NotNull Integer code2FA,
        @Context org.jboss.resteasy.spi.HttpRequest request
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        TwoFactorAuthParameters params2FA = RestParametersParser.get2FARequestAttribute(request);

        account2FAHelper.confirm2FA(params2FA);
        Account2FADTO dto = faConverter.convert(params2FA);

        return response.bind(dto).build();
    }


    @Path("/disable")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "Disable two factor authentication",
        description = "Disable two factor authentication.",
        tags = {"accounts"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "text/html",
                    schema = @Schema(implementation = Account2FADTO.class)))
        })

    @PermitAll
    @Secured2FA
    public Response disable2FA(
        @Parameter(description = "The passphrase") @FormParam("passphrase") String passphraseParam,
        @Parameter(description = "The secret phrase") @FormParam("secretPhrase") String secretPhraseParam,
        @Parameter(description = "The account ID.") @FormParam("account") String accountStr,
        @Parameter(description = "The 2FA code.", required = true) @FormParam("code2FA") @NotNull Integer code2FA,
        @Context org.jboss.resteasy.spi.HttpRequest request
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        TwoFactorAuthParameters params2FA = RestParametersParser.get2FARequestAttribute(request);

        account2FAHelper.disable2FA(params2FA);

        Account2FADTO dto = faConverter.convert(params2FA);

        return response.bind(dto).build();
    }

    @Path("/enable")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "Enable two factor authentication",
        description = "Enable two factor authentication.",
        tags = {"accounts"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "text/html",
                    schema = @Schema(implementation = Account2FADetailsDTO.class)))
        })
    @PermitAll
    public Response enable2FA(
        @Parameter(description = "The passphrase") @FormParam("passphrase") String passphraseParam,
        @Parameter(description = "The secret phrase") @FormParam("secretPhrase") String secretPhraseParam,
        @Parameter(description = "The account ID.") @FormParam("account") String accountStr
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        TwoFactorAuthParameters params2FA = account2FAHelper.create2FAParameters(accountStr, passphraseParam, secretPhraseParam, null);

        TwoFactorAuthDetails twoFactorAuthDetails = account2FAHelper.enable2FA(params2FA);

        Account2FADetailsDTO dto = faDetailsConverter.convert(twoFactorAuthDetails);
        faDetailsConverter.addAccount(dto, params2FA.getAccountId());

        return response.bind(dto).build();
    }

}
