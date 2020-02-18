/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto.utils;

import javax.validation.constraints.NotBlank;
import javax.ws.rs.FormParam;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class DecodeQRCodeRequestDto {
//    @Parameter(name = "qrCodeBase64", description = "A base64 string encoded from an image of a QR code", required = true)
    @FormParam("qrCodeBase64")
    @NotBlank
    public String qrCodeBase64;
}
