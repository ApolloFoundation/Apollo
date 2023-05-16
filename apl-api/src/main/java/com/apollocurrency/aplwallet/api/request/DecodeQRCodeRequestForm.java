/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.FormParam;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class DecodeQRCodeRequestForm {
    //    @Parameter(name = "qrCodeBase64", description = "A base64 string encoded from an image of a QR code", required = true)
    @FormParam("qrCodeBase64")
    @NotBlank
    public String qrCodeBase64;
}
