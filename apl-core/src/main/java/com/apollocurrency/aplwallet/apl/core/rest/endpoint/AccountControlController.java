/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.PositiveOrZero;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


import com.apollocurrency.aplwallet.api.dto.account.AccountAssetDTO;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.rest.utils.FirstLastIndexParser;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
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
import lombok.extern.slf4j.Slf4j;

@OpenAPIDefinition(info = @Info(description = "Provide methods to operate with accounts"))
@SecurityScheme(type = SecuritySchemeType.APIKEY, name = "admin_api_key", in = SecuritySchemeIn.QUERY, paramName = "adminPassword")
@NoArgsConstructor
@Slf4j
@Path("/accounts/control")
public class AccountControlController {

    private Blockchain blockchain;
    private FirstLastIndexParser indexParser;

    @Inject
    public AccountControlController(Blockchain blockchain, FirstLastIndexParser indexParser) {
        this.blockchain = blockchain;
        this.indexParser = indexParser;
    }

    @Path("/phasing")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get all phasing only entities by using first/last index",
        description = "Get all phasing only entities by using first/last index",
        tags = {"accounts"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AccountAssetDTO.class)))
        })
    @PermitAll
    public Response getAccountAssets(
        @Parameter(description = "A zero-based index to the first asset ID to retrieve (optional).")
        @QueryParam("firstIndex") @DefaultValue("0") @PositiveOrZero int firstIndex,
        @Parameter(description = "A zero-based index to the last asset ID to retrieve (optional).")
        @QueryParam("lastIndex") @DefaultValue("-1") int lastIndex
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        FirstLastIndexParser.FirstLastIndex flIndex = indexParser.adjustIndexes(firstIndex, lastIndex);
        AccountAssetDTO dto = new AccountAssetDTO();

        return response.bind(dto).build();
    }


}
