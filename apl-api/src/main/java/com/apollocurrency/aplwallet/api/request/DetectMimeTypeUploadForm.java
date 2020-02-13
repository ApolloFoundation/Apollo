/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.request;

import javax.ws.rs.FormParam;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

/**
 * Form data submitted to detect mime-type for file or 'data' field content
 * That class is used for OpenAPI displaying mostly. DO NOT REMOVE !!
 */
@NoArgsConstructor
public class DetectMimeTypeUploadForm {

    @FormParam("file")
    @PartType("charset=utf-8")
    @Schema(type = "string", format = "binary", description = "Uploaded file name, optional, limited by Max Upload Size (42 Kb by default)")
    public String file;

    @FormParam("data")
    @PartType("text/plain;charset=utf-8")
    @Schema(type = "string", description = "Raw Data as string for mime-type detection, optional")
    public String data;
}
