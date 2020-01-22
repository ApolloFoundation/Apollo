package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.SymbolsOutputDTO;
import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.apollocurrency.aplwallet.api.dto.TradingViewConfigDTO;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutputUpdated;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TradingDataOutputUpdatedToDtoConverter;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.exchange.service.graph.DexTradingDataService;
import com.apollocurrency.aplwallet.apl.exchange.service.graph.TimeFrame;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import static com.apollocurrency.aplwallet.apl.exchange.utils.TradingViewUtils.getUpdatedDataForIntervalFromOffers;

@Singleton
@Path("/dex/chart")
@OpenAPIDefinition(info = @Info(description = "Provide dex trading data (candlesticks) for ETH, PAX, etc"))
@Slf4j
public class TradingDataController {
    private static final String BASE_CURRENCY_ERROR = "Base currency should be equal to 'APL'";
    private static final String PAIRED_CURRENCY_ERROR = "Paired currency should not be equal to 'APL'";

    private DexTradingDataService dataService;
    private TimeService timeService;
    private DexService dexService;

    @Inject
    public TradingDataController(DexTradingDataService dataService, TimeService timeService, DexService dexService) {
        this.dataService = dataService;
        this.timeService = timeService;
        this.dexService = dexService;
    }

    private TradingDataOutputUpdatedToDtoConverter converter = new TradingDataOutputUpdatedToDtoConverter();
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
        TradingDataOutputUpdated tradingDataOutput = dataService.getBars(toTs, limit, tsym, timeFrame);
        return Response.ok( converter.apply(tradingDataOutput) ) .build();

    }

    @GET
    @Path("/history")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get history", description = "getting history")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Exchange offers"),
        @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getHistoday2(   @Parameter(description = "Cryptocurrency identifier") @QueryParam("symbol") String symbol,
                                    @Parameter(description = "resolution") @QueryParam("resolution") String resolution,
                                    @Parameter(description = "from") @QueryParam("from") Integer from,
                                    @Parameter(description = "to") @QueryParam("to") Integer to,
                                    @Context HttpServletRequest req) throws NotFoundException {

        log.debug("getHistory:  fsym: {}, resolution: {}, to: {}, from: {}", symbol, resolution, to, from);


        // the date of DEX release - 30.09.. taking 25 as an upper limit
        //1569369600
        //Is equivalent to: 09/25/2019 @ 12:00am (UTC)

        if (to <= 1569369600){
            log.debug("flushing: ");
            TradingDataOutputUpdated tdo = new TradingDataOutputUpdated();
            tdo.setC(null);
            tdo.setH(null);
            tdo.setL(null);
            tdo.setO(null);
            tdo.setT(null);
            tdo.setV(null);
            tdo.setNextTime(null);
            tdo.setS("no_data");
            return Response.ok( new TradingDataOutputUpdatedToDtoConverter().apply(tdo) ) .build();
        }


        TradingDataOutputUpdated tradingDataOutputUpdated = getUpdatedDataForIntervalFromOffers(symbol,resolution,to,from, dexService, timeService);
        return Response.ok( new TradingDataOutputUpdatedToDtoConverter().apply(tradingDataOutputUpdated) ) .build();
    }


    @GET
    @Path("/symbols")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get history", description = "getting history")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Exchange offers"),
        @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getSymbols(   @Parameter(description = "Cryptocurrency identifier") @QueryParam("symbol") String symbol,
                                  @Context HttpServletRequest req) throws NotFoundException {

        log.debug("getSymbols:  fsym: {}", symbol );
        TimeZone tz = Calendar.getInstance().getTimeZone();
        SymbolsOutputDTO symbolsOutputDTO = new SymbolsOutputDTO();
        symbolsOutputDTO.name = symbol;
        symbolsOutputDTO.exchange_traded = "Apollo DEX";
        symbolsOutputDTO.exchange_listed = "Apollo DEX";
        symbolsOutputDTO.timezone = tz.getID();
        symbolsOutputDTO.minmov = 1;
        symbolsOutputDTO.minmov2 = 0;
        symbolsOutputDTO.pointvalue = 1;
        symbolsOutputDTO.session = "24x7";
        symbolsOutputDTO.has_intraday = true;
        symbolsOutputDTO.has_no_volume = false;
        symbolsOutputDTO.has_daily = true;
        symbolsOutputDTO.description = symbol;
        symbolsOutputDTO.type = "cryptocurrency";
        symbolsOutputDTO.has_empty_bars = true;
        symbolsOutputDTO.has_weekly_and_monthly = false;
        symbolsOutputDTO.supported_resolutions = new ArrayList<>();;
        symbolsOutputDTO.supported_resolutions.add("15");
        symbolsOutputDTO.supported_resolutions.add("60");
        symbolsOutputDTO.supported_resolutions.add("240");
        symbolsOutputDTO.supported_resolutions.add("D");
        symbolsOutputDTO.pricescale = 1000000000;
        symbolsOutputDTO.ticker = symbol;

        return Response.ok( symbolsOutputDTO ) .build();
    }

    @GET
    @Path("/time")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get time for trading vies", description = "getting time")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Exchange offers"),
        @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getTime(  ) throws NotFoundException {

        Long time = System.currentTimeMillis()/1000L;
        return Response.ok( time ) .build();
    }



    @GET
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get configuration", description = "getting TV configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Exchange offers"),
        @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getConfig(  ) throws NotFoundException {

        log.debug("getConfig entry point");
        TradingViewConfigDTO tradingViewConfigDTO = new TradingViewConfigDTO();
        tradingViewConfigDTO.supports_search = true;
        tradingViewConfigDTO.supports_group_request = false;
        tradingViewConfigDTO.supports_marks = false;
        tradingViewConfigDTO.supports_timescale_marks = false;
        tradingViewConfigDTO.supports_time = false;
        // resolutions
        tradingViewConfigDTO.supported_resolutions =  new ArrayList<>();
        tradingViewConfigDTO.supported_resolutions.add("15");
        tradingViewConfigDTO.supported_resolutions.add("60");
        tradingViewConfigDTO.supported_resolutions.add("240");
        tradingViewConfigDTO.supported_resolutions.add("D");

        return Response.ok( tradingViewConfigDTO ) .build();
    }
}
