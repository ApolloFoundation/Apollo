/*
 * Copyright © 2018 - 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author Serhiy Lymar
 */

@JsonSerialize
@Data
public class TradingViewConfigDTO {

    @Schema(name = "supports_search", description = "search support in TV")
    public boolean supports_search;
    @Schema(name = "supports_group_request", description = "support group request in TV")
    public boolean supports_group_request;
    @Schema(name = "supports_marks", description = "marks support in TV")
    public boolean supports_marks;
    @Schema(name = "supports_timescale_marks", description = "timescale marks support in TV")
    public boolean supports_timescale_marks;
    @Schema(name = "supports_time", description = "time support in TV")
    public boolean supports_time;
    @Schema(name = "exchanges", description = "different exchanges for TV")
    public List<TradingViewExchangesDTO> exchanges;
    @Schema(name = "symbols_types", description = "different exchanges for TV")
    public List<TradingViewSymbolTypesDTO> symbols_types;
    @Schema(name = "supported_resolutions", description = "supported resolutions for TV")
    public List<String> supported_resolutions;
}