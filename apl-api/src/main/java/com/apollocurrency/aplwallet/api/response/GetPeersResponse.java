package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class GetPeersResponse extends ResponseBase {

    private List<PeerDTO> peers;

}
