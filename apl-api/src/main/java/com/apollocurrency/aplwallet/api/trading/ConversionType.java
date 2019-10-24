/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.trading;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author Serhiy Lymar
 */

@NoArgsConstructor

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ConversionType", description = "Conversion type")

public class ConversionType {
    public String type;
    public String conversionSymbol;
}
