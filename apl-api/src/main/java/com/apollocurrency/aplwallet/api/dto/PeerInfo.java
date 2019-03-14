package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.api.response.ResponseBase;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * @author al
 */
@Schema(name="PeerInfo", description="Sample model for the documentation")
public class PeerInfo extends ResponseBase {
    public String address;
    public String host;

}
