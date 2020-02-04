/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import javax.imageio.ImageIO;
import javax.inject.Singleton;
import javax.validation.constraints.PositiveOrZero;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.apollocurrency.aplwallet.api.dto.BaseDTO;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
@Path("/utils")
@OpenAPIDefinition(info = @Info(description = "Provide several utility methods used by UI"))
@Singleton
public class UtilsController {

    @Path("/qrcode/encode")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The EncodeQRCode API converts a UTF-8 string to a base64-encoded jpeg image of a 2-D QR (Quick Response) code",
        description = "The output qrCodeBase64 string can be incorporated into an in-line HTML image like this: &lt;img src=\"data:image/jpeg;base64,qrCodeBase64\"&gt",
        tags = {"utility"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = QrCodeDto.class)))
        })
    public Response encodeQRCode(
        @Parameter(description = "QR code data", required = true) @QueryParam("qrCodeData") String qrCodeData,
        @Parameter(description = "QR code image width", allowEmptyValue = true) @QueryParam("width") @PositiveOrZero @DefaultValue("100") int width,
        @Parameter(description = "QR code image height", allowEmptyValue = true) @QueryParam("height") @PositiveOrZero @DefaultValue("100") int height
    ) {
        log.debug("Started encodeQRCode,\n\t\twidth={}, height={}, qrCodeData={}", width, height, qrCodeData);
        ResponseBuilder response = ResponseBuilder.startTiming();
        QrCodeDto dto = new QrCodeDto();
        try {
//            Map<EncodeHintType, Object> hints = new HashMap<>();
            Map hints = new HashMap();
            // Error correction level: L (7%), M (15%), Q (25%), H (30%) -- Default L.
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 0); // Default 4
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = new MultiFormatWriter().encode(qrCodeData,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            );
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpeg", os);
            byte[] bytes = os.toByteArray();
            os.close();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            log.debug("base64 = {}", base64);
            dto.qrCodeBase64 = base64;
        } catch(WriterException | IOException ex) {
            String errorMessage = String.format("Error creating image from qrCodeData: %s", qrCodeData);
            log.error(errorMessage, ex);
//            return response.error(new ApiErrors(4, 0, errorMessage).).build();
            return response.error(ApiErrors.JSON_SERIALIZATION_EXCEPTION).build();
//            JSONData.putException(response, ex, errorMessage);
        }
        return response.bind(dto).build();
    }

    @NoArgsConstructor
    public static class QrCodeDto extends BaseDTO {
        public String qrCodeBase64;
    }


}
