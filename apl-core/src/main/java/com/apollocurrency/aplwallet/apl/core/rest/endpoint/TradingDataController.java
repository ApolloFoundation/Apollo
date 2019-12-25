package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TradingDataOutputToDtoConverter;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.service.graph.DexTradingDataService;
import com.apollocurrency.aplwallet.apl.exchange.service.graph.TimeFrame;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@Path("/dex/chart")
@OpenAPIDefinition(info = @Info(description = "Provide dex trading data (candlesticks) for ETH, PAX, etc"))
public class TradingDataController {
    private static final String BASE_CURRENCY_ERROR = "Base currency should be equal to 'APL'";
    private static final String PAIRED_CURRENCY_ERROR = "Paired currency should not be equal to 'APL'";
    @Inject
    @Setter
    private DexTradingDataService service;
    private TradingDataOutputToDtoConverter converter = new TradingDataOutputToDtoConverter();
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get candlesticks, APL/ETH, APL/PAX, etc",
            description = "Retrieve candlesticks using buy orders history and stored candlesticks for specified currency pair. Quantity is specified by 'limit' parameter ",
            responses = @ApiResponse(description = "trading data output with request parameters and candlesticks data",
                    content = @Content(schema = @Schema(implementation = TradingDataOutputDTO.class), mediaType = "application/json"))
    )
    public Response getCandlesticks(
                                    @Parameter(description = "First currency symbol in the trading pair, should always be APL") @DefaultValue("APL") @QueryParam("fsym") DexCurrency fsym,
                                    @Parameter(description = "Second currency symbol in the trading pair, for example - ETH, PAX, etc") @QueryParam("tsym") DexCurrency tsym,
                                    @Parameter(description = "Timestamp, which restrict candlesticks time from upper bound") @QueryParam("toTs") Integer toTs,
                                    @Parameter(description = "Number of candlesticks to return") @QueryParam("limit") Integer limit,
                                    @Parameter(description = "Time frame for which trading candlesticks should be returned. Possible values: QUARTER, HOUR, FOUR_HOURS, DAY ")  @QueryParam("timeFrame") TimeFrame timeFrame
    ) throws ParameterException {

        if (fsym != DexCurrency.APL) {
            throw new ParameterException(BASE_CURRENCY_ERROR, null, JSONResponses.error(BASE_CURRENCY_ERROR));
        }
        if (tsym == DexCurrency.APL) {
            throw new ParameterException(PAIRED_CURRENCY_ERROR, null, JSONResponses.error(PAIRED_CURRENCY_ERROR));
        }
        TradingDataOutput tradingDataOutput = service.getForTimeFrame(toTs, limit, tsym, timeFrame);
        return Response.ok( converter.apply(tradingDataOutput) ) .build();

    }
}
